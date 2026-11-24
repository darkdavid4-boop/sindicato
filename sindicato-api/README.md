# API del Sindicato Ficticio de Inquilinos

Monorepo Maven con cinco microservicios Spring Boot, un módulo compartido de eventos, MySQL independiente por servicio, Kafka, envío SMTP y Resilience4j.

## Puesta en marcha

Requiere Docker Compose y Java 21/Maven 3.9 para la compilación local. La web se publica en `http://localhost:3000`. Configura `APP_AUTH_SECRET` como una clave aleatoria de al menos 32 bytes codificada en Base64; el valor integrado en Compose es solo para desarrollo local.

```powershell
docker compose up --build
```

Compose levanta MySQL, Kafka en modo KRaft, los cinco servicios y Mailpit para probar el correo localmente. MySQL se expone en el puerto `3307` del host (configurable mediante `MYSQL_PORT`), y Mailpit queda disponible en `http://localhost:8025`. Para usar SMTP real, configura `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` y `SMTP_STARTTLS` en el entorno. `MYSQL_ROOT_PASSWORD` y `APP_PUBLIC_URL` también se pueden personalizar.

Los administradores deben tener una cuenta afiliada activa; su rol lo determina `APP_ADMIN_EMAILS` (emails separados por comas). Documentación requiere además `APP_DOCUMENTOS_KEY` (clave AES-256 codificada en Base64). Gestiona y respalda la clave de cifrado en un gestor de secretos: perderla impide recuperar los archivos.

Para generar una clave local en PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Define `APP_ADMIN_EMAILS` y `APP_DOCUMENTOS_KEY` en el entorno o en `.env`; no guardes la clave en el repositorio.

Los servicios publican sus comprobaciones de estado en `/actuator/health`. Los servicios con API documentada ofrecen Swagger UI en `/swagger-ui.html`.

| Servicio | URL | Responsabilidad |
|---|---|---|
| Afiliados | `http://localhost:8081` | Registro, datos, confirmación de alta y solicitud/confirmación de baja |
| Noticias | `http://localhost:8082` | CRUD, noticias paginadas y noticias asociadas a actividades |
| Actividades | `http://localhost:8083` | CRUD, inscripción y cancelación de participaciones |
| Notificaciones | interno | Consumidor de eventos que envía y distribuye emails |
| Documentación | `http://localhost:8085` | Publicación, previsualización y descarga de documentos públicos |
| Web | `http://localhost:3000` | Portada, noticias, actividades, documentos y afiliación |

Swagger UI de documentación: `http://localhost:8085/swagger-ui.html`. Pulsa **Authorize** y pega el JWT obtenido al iniciar sesión.

## Rutas principales

### Afiliados

- `POST /api/afiliados` — crear en estado `PENDIENTE`, establecer contraseña y enviar confirmación.
- También admite `multipart/form-data` con `datos` (JSON) e `imagen` (PNG, JPG o GIF; máximo 5 MB) para guardar la foto de perfil.
- `POST /api/afiliados/sesion` con `{"email":"ana@example.com","password":"..."}` — inicia sesión si la afiliación está confirmada (`ALTA`) y devuelve un JWT válido durante cuatro horas, junto con el rol `ADMIN` o `AFILIADO`.
- `GET /api/afiliados/{id}`, `GET /api/afiliados?pagina=0` — consulta; páginas fijas de 10.
- `PUT /api/afiliados/{id}`, `DELETE /api/afiliados/{id}`.
- `POST /api/afiliados/{id}/baja` — enviar enlace para confirmar la baja.
- `GET /api/afiliados/confirmar?token=...` — confirmar alta o baja.

El DNI/NIE español valida el formato y la letra de control; el email usa validación estándar y el teléfono es opcional. La contraseña requiere al menos 10 caracteres y admite hasta 72 bytes; se guarda como hash BCrypt y nunca se devuelve en las respuestas. Para cuentas antiguas sin contraseña se puede establecer al actualizar el afiliado.
La contraseña requiere al menos 10 caracteres y admite hasta 72 bytes; se guarda como hash BCrypt y nunca se devuelve en las respuestas. Para cuentas antiguas sin contraseña se puede establecer al actualizar el afiliado.

### Roles y autorización JWT

| Rol | Lectura pública | Publicar noticias, actividades y documentos | Eliminar noticias, actividades y documentos |
|---|---|---|---|
| Público (sin sesión) | Sí | No | No |
| Afiliado (`ALTA`) | Sí | Sí | No |
| Admin (`ALTA` y correo en `APP_ADMIN_EMAILS`) | Sí | Sí | Sí |

El backend aplica estos permisos en cada microservicio; ocultar botones en la web no sustituye la autorización del servidor. La edición (`PUT`) y eliminación son solo para Admin; las inscripciones a actividades requieren un JWT de Afiliado.

#### Cómo se autentica el JWT, paso a paso

1. La persona envía email y contraseña a `POST /api/afiliados/sesion`. El servicio Afiliados valida la contraseña BCrypt y exige que la afiliación esté activa (`ALTA`).
2. Afiliados determina el rol: `ADMIN` si el correo aparece en `APP_ADMIN_EMAILS`; en otro caso, `AFILIADO`. Emite un JWT con `iss=sindicato`, el ID de afiliado en `sub`, el nombre, las autoridades `ROLE_ADMIN` o `ROLE_AFILIADO`, la fecha de emisión y la expiración a cuatro horas.
3. Afiliados firma el token con HS256 y `APP_AUTH_SECRET`. La web conserva el token en la sesión del navegador y lo adjunta como `Authorization: Bearer <JWT>` a las peticiones que modifican contenido.
4. Noticias, Actividades y Documentación actúan como *resource servers*: cada uno usa el mismo `APP_AUTH_SECRET` para comprobar la firma, el emisor y las fechas del token antes de ejecutar una ruta protegida. No reciben la contraseña ni vuelven a iniciar sesión con ella.
5. Cada servicio convierte la autoridad firmada en permisos y decide: publicar requiere `ROLE_AFILIADO` o `ROLE_ADMIN`; eliminar requiere `ROLE_ADMIN`. Las consultas públicas de noticias, actividades y documentos no necesitan token.

La web llama a cada servicio pasando el JWT; no hay una llamada HTTP de autenticación entre estos servicios. La creación automática de una noticia al crear una actividad sigue ocurriendo como evento interno de Kafka, no como una petición de usuario. Para producción, mantén el secreto fuera del repositorio, usa HTTPS en todos los saltos y considera claves asimétricas si se requiere rotación independiente por servicio.

### Noticias

- `POST /api/noticias`, `GET /api/noticias/{id}`, `GET /api/noticias?pagina=0`.
- Para publicar con imagen, `POST /api/noticias` admite `multipart/form-data` con `datos` (JSON) e `imagen` (PNG, JPG o GIF; máximo 5 MB).
- `GET /api/noticias/actividades` — noticias con `actividadId` informado.
- `PUT /api/noticias/{id}`, `DELETE /api/noticias/{id}` — solo Admin.

### Actividades y participaciones

- `POST /api/actividades`, `GET /api/actividades/{id}`, `GET /api/actividades?pagina=0`.
- Para crear con imagen, `POST /api/actividades` admite `multipart/form-data` con `datos` (JSON) e `imagen` (PNG, JPG o GIF; máximo 5 MB). La noticia generada automáticamente reutiliza la imagen de la actividad.
- `PUT /api/actividades/{id}`, `DELETE /api/actividades/{id}` — solo Admin.
- `POST /api/actividades/{id}/participaciones` con `Authorization: Bearer <JWT>` — inscribe al afiliado autenticado; no se acepta un ID de afiliado en el cuerpo.
- `GET /api/actividades/{id}/participaciones`.
- `DELETE /api/actividades/{id}/participaciones/{participacionId}`.

Crear una actividad publica un evento que el servicio de noticias convierte en noticia. Las inscripciones reservan plazas bajo bloqueo transaccional; al completar capacidad, la actividad pasa a `LLENO`, y nuevas inscripciones reciben HTTP 409. Las participaciones pendientes existentes se cancelan al completarse.

## Eventos Kafka

- `afiliados.creados` y `afiliados.bajas-solicitadas`: solicitud de email de confirmación.
- `afiliados.estados`: sincroniza destinatarios activos del servicio de notificaciones.
- `actividades.creadas`: crea una noticia asociada a la actividad.
- `noticias.publicadas`: notifica a los afiliados en estado `ALTA`.

Las APIs devuelven errores JSON controlados para datos no válidos, recursos/listados inexistentes, actividad llena y fallos de dependencias conocidos. Los endpoints de salud reflejan disponibilidad de base de datos, Kafka y SMTP según los indicadores de Spring Boot Actuator.

Las imágenes se almacenan en volúmenes Docker independientes por servicio y se sirven desde `/media/afiliados/`, `/media/noticias/` y `/media/actividades/`. Fuera de Docker, se guardan en la carpeta `uploads` del servicio; `APP_UPLOAD_DIR` permite cambiar esa ubicación.

## Documentos públicos

El servicio acepta documentos PDF, JPG y PNG de hasta 20 MB. Los archivos se cifran con AES-256-GCM y se guardan con un nombre aleatorio; cada documento conserva su tipo, descripción, nombre original, tamaño y fecha. Cualquier visitante puede consultar la biblioteca, previsualizar y descargar los documentos publicados.

- `GET /api/documentos` y `GET /api/documentos/{id}` — consultar documentos y sus metadatos publicados.
- `GET /api/documentos/{id}/previsualizacion` y `GET /api/documentos/{id}/archivo` — mostrar en línea o descargar el archivo.
- `POST /api/documentos` — Admin o Afiliado; `multipart/form-data` con `datos` (JSON con `tipo` — `CONTRATO_ALQUILER`, `RECIBO_ALQUILER`, `FACTURA`, `DESPERFECTOS_VIVIENDA`, `DENUNCIA` u `OTRO` — y `descripcion` opcional) y `archivo`. La carga queda publicada inmediatamente.
- `DELETE /api/documentos/{id}` — solo Admin.

Los documentos que ya existían antes de esta actualización permanecen ocultos: la migración añade `publicado=false` a los registros anteriores, mientras que las cargas nuevas se publican. No publiques archivos con datos personales o información confidencial.
