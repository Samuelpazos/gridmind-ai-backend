# Verificación del backend

Con Java 25 y Maven disponibles, ejecuta `mvn test`. Docker permite que Testcontainers levante PostgreSQL para verificar Flyway, el esquema JPA y la persistencia.

Comprobaciones funcionales:

1. Inicia una sesión de administrador y confirma que el cierre de sesión invalida el acceso.
2. Crea un transformador simulado y ejecuta un escenario con un valor absoluto.
3. Registra un transformador real sin especificaciones y vincula su sensor de temperatura.
4. Archiva y restaura un equipo de prueba. Comprueba que vuelva al inventario activo.
5. Ejecuta una acción reversible y comprueba que **Deshacer** restaure su estado anterior.
