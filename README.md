# AvilaOS

Simulador de un cluster con RAM independiente por computador, admision de procesos, CPU con FCFS y reloj global coordinado por semaforos.

## Ejecutar en NetBeans

1. Instalar y registrar JDK 22 en las plataformas Java de NetBeans.
2. Abrir esta carpeta como proyecto Maven y seleccionar JDK 22 para el proyecto.
3. Ejecutar `Main.Proyecto1SO2526`. La ventana se crea en el EDT de Swing.
4. En Configuracion indicar al menos dos computadores, su RAM separada por comas y la duracion del ciclo. Aplicar la configuracion antes de crear procesos.
5. Crear procesos CPU_BOUND en la pestaña de procesos y controlar el reloj con Iniciar, Pausar, Reanudar y Detener. Despues de detener, aplicar o cargar una configuracion para comenzar otra sesion.
6. Consultar CPU, PCB, RAM y colas en la pestaña de computadores. Cerrar la ventana solicita el cierre del reloj fuera del EDT.

El proyecto usa Java 22 y no necesita bibliotecas externas. Los archivos `.form` de cada panel se editan en el diseñador de NetBeans; la ventana principal ensambla estos paneles en tiempo de ejecucion.

## Guardar y cargar

Para probar sin introducir datos, pulsar **Cargar ejemplo** y luego **Iniciar**. El paquete `Ejemplos` ofrece `ConfiguracionInicial.crear()`; el mismo escenario esta en `ejemplos/cluster-inicial.csv` para cargarlo con el selector. Incluye dos computadores, cinco procesos y un buffer de metadatos. En el computador 1, Informe espera RAM hasta que termina Compilar. El ciclo dura 700 ms para observar los movimientos. Al finalizar permanecen las ocho unidades reservadas por el buffer Datos.

En Simulacion en vivo, el selector permite enfocar un computador. Las tarjetas muestran memoria, instrucciones restantes, plazo y avance; activar **Detalles PCB** para ver PC, MAR y prioridad. El desplazamiento de cada cola se conserva durante el refresco.

La configuracion explica la RAM por equipo y la duracion del ciclo; destaca el campo que debe corregirse antes de aplicar. Los parametros de integracion se despliegan bajo demanda. El resumen de computadores muestra CPU activas, procesos en espera, terminados y RAM total sin reemplazar las lecturas individuales de cada equipo.

La ventana abre dentro del area util de la pantalla, sin maximizar. Los controles del reloj usan dos filas; los formularios se pueden recorrer verticalmente y las colas se distribuyen en dos columnas cuando el ancho es reducido. Procesos comparte el estilo y las ayudas de Configuracion. FCFS es la unica politica ejecutable: Round Robin, EDF y Prioridades figuran como pendientes, sin simular su ejecucion.

Los botones de archivos guardan y leen CSV UTF-8 version 1. Se conserva la configuracion y la carga **inicial**: cargar reconstruye los procesos con PC y ciclo cero, nuevos identificadores y sus instrucciones y deadlines originales. No es una reanudacion del estado de ejecucion. El archivo completo se valida antes de reemplazar la sesion; detener el reloj antes de cargar.

Los registros admitidos son `AVILAOS`, `CONFIG`, `COMPUTADOR`, `PROCESO` y `BUFFER`. Los nombres pueden contener comas, comillas y saltos de linea mediante el escape CSV. Los buffers cargados reservan una unidad de RAM por elemento en su anfitrion; sus registros son metadatos y no ejecutan productor/consumidor.

## Integracion

- `Modelo`: PCB, configuracion, definiciones iniciales y vistas inmutables.
- `EDD`: cola enlazada propia protegida por semaforo.
- `Sistema.Computador`: admision, CPU, colas, deadlines y liberacion/readmision de RAM al terminar.
- `Sistema.RelojGlobal`: coordina un avance por computador y ciclo; propaga fallos y cierra trabajadores.
- `Sistema.ControladorSimulacion`: construye y controla sesiones sin depender de Swing.
- `Interfaces`: formularios y lectura de vistas. Las operaciones que esperan trabajadores o acceden a archivos se realizan fuera del EDT.
- `Persistencia`: codec CSV y validacion de cargas iniciales, independientes de las ventanas.

Las vistas son coherentes por computador. Durante un ciclo en curso, dos computadores pueden mostrar ciclos diferentes hasta que ambos confirmen su avance. No modificar las colas de un computador desde la GUI: usar las operaciones del controlador y del computador.

La implementacion ejecuta CPU_BOUND con FCFS. Quantum y latencia se validan y conservan como configuracion para integraciones posteriores; no activan Round Robin, comunicaciones ni cambios de contexto simulados. IO, productor/consumidor, politicas avanzadas y metricas corresponden a otros modulos y no deben enviarse a esta CPU como si fueran CPU_BOUND.

## Pruebas y ramas

Para comprobar el almacenamiento de buffers y su registro con RAM desde PowerShell,
ejecutar `./verificar-buffers.ps1 -Jdk 'C:/Program Files/Java/jdk-22'` (ajustar la ruta
al JDK instalado). Compila el proyecto y las comprobaciones de `src/test/java/Pruebas`
sin dependencias externas. Verifica capacidad, FIFO, acceso concurrente, reservas
en el anfitrion, rechazos sin cambios, admision de procesos y reconstruccion CSV.
El script devuelve un error si falla una comprobacion; no abre la interfaz grafica.

Las pruebas recientes estan comentadas en `Main.Proyecto1SO2526.java`, con prints y comprobaciones mediante `AssertionError`. Para repetir una, desactivar temporalmente el lanzamiento de la ventana, activar su bloque, compilar y ejecutar; restaurar despues el lanzamiento y el comentario. Las salidas y archivos temporales se escriben en `target`, ignorado por Git.

Se conservan tres escenarios: flujo completo desde las acciones Swing, ventana de 900x680 sin maximizar y estabilidad del scroll. El flujo completo carga el ejemplo, crea un sexto proceso desde el formulario, cambia la duracion, inicia/pausa/reanuda/detiene, comprueba finalizacion y RAM, guarda/abre CSV y rechaza un archivo corrupto sin perder la sesion. Al cerrar verifica que no quedan trabajadores del reloj. Los archivos de esta prueba se eligen por ruta desde main, sin automatizar el dialogo del selector.

Las ramas locales del domingo dependen en este orden: `feature/interfaz-configuracion` → `feature/visualizacion-computadores` → `feature/persistencia`. Se deben revisar e integrar respetando ese orden. La documentacion describe estos componentes; no sustituye el informe ni el UML de la entrega completa.
