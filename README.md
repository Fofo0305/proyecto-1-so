# AvilaOS

Simulador de un cluster con RAM independiente por computador, admision de procesos, CPU con cuatro politicas de planificacion (FCFS, Round Robin, EDF y Prioridades) y reloj global coordinado por semaforos. Incluye buffers compartidos con semaforos simulados, productores y consumidores locales o remotos, procesos con E/S, log de eventos y metricas por computador.

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

La ventana abre dentro del area util de la pantalla, sin maximizar. Los controles del reloj usan dos filas; los formularios se pueden recorrer verticalmente y las colas se distribuyen en dos columnas cuando el ancho es reducido. Procesos comparte el estilo y las ayudas de Configuracion. Las cuatro politicas son ejecutables y se eligen **por computador**: FCFS, Round Robin con quantum, EDF por deadline mas cercano y Prioridades por menor numero. Round Robin, EDF y Prioridades piden apropiacion; FCFS no la pide nunca.

Los botones de archivos guardan y leen CSV UTF-8. La version se elige segun el contenido: **3** cuando hay procesos `IO_BOUND` o algun computador usa una politica distinta de FCFS o un quantum propio, **2** cuando hay productores o consumidores, y **1** para cargas CPU-bound; se leen las tres versiones. Se conserva la configuracion y la carga **inicial**: cargar reconstruye los procesos con PC y ciclo cero, nuevos identificadores y sus instrucciones y deadlines originales. No es una reanudacion del estado de ejecucion. El archivo completo se valida antes de reemplazar la sesion; detener el reloj antes de cargar.

Los registros admitidos son `AVILAOS`, `CONFIG`, `COMPUTADOR`, `PROCESO` y `BUFFER`. Los nombres pueden contener comas, comillas y saltos de linea mediante el escape CSV. Los buffers cargados reservan una unidad de RAM por posicion en su anfitrion y comienzan vacios. En version 2 los registros de productor/consumidor agregan nombre de buffer, intervalo y elementos objetivo despues del tipo. En version 3 cada `COMPUTADOR` agrega su politica y su quantum, y los procesos `IO_BOUND` agregan rafaga de CPU y duracion de la espera despues del tipo.

## Integracion

- `Modelo`: PCB, configuracion, definiciones iniciales y vistas inmutables.
- `EDD`: cola enlazada propia protegida por semaforo.
- `Planificacion`: interfaz `Politica` y las cuatro politicas. `Ordenada` da la base estable de EDF y Prioridades; los empates conservan el orden de llegada a listos.
- `Sistema.Computador`: admision, CPU, colas, deadlines, E/S, log de eventos, metricas y liberacion/readmision de RAM al terminar.
- `Sistema.RelojGlobal`: coordina un avance por computador y ciclo; propaga fallos y cierra trabajadores.
- `Sistema.ControladorSimulacion`: construye y controla sesiones sin depender de Swing.
- `Interfaces`: formularios y lectura de vistas. Las operaciones que esperan trabajadores o acceden a archivos se realizan fuera del EDT.
- `Persistencia`: codec CSV y validacion de cargas iniciales, independientes de las ventanas.

Las vistas son coherentes por computador. Durante un ciclo en curso, dos computadores pueden mostrar ciclos diferentes hasta que ambos confirmen su avance. No modificar las colas de un computador desde la GUI: usar las operaciones del controlador y del computador.

La implementacion ejecuta CPU_BOUND, IO_BOUND, PRODUCTOR y CONSUMIDOR, locales o remotos, con cualquiera de las cuatro politicas. La latencia de red se aplica a cada operacion remota y el quantum solo lo consume Round Robin. La apropiacion ocurre dentro de `avanzarCiclo`: la politica decide con `debeApropiar`, el proceso retirado vuelve a listos conservando su contexto y queda un evento `APROPIACION`. `cederCPU` sigue disponible como primitiva manual para forzar un cambio de contexto; el nucleo no la invoca por si mismo.

`ControladorSimulacion.cambiarPolitica(equipo, tipo, quantum)` cambia la politica de un computador con el reloj en marcha y `crearProcesoES(...)` crea un proceso `IO_BOUND` con su rafaga y la duracion de su espera. Ambas quedan en la configuracion, por lo que se guardan en el CSV version 3.

`Sistema.RegistroSistema` es un anillo acotado de 1024 eventos por computador y registra el ciclo de vida del proceso (`CREACION`, `ADMISION`, `DESPACHO`, `APROPIACION`, `BLOQUEO_ES`, `FIN_ES`, `TERMINACION`) y los cambios de politica (`POLITICA`). Tambien recibe los sucesos del motor de buffers de `TipoEventoBuffer` (`PRODUCCION`, `CONSUMO`, bloqueos y despertares de semaforo y de red, `DEVOLUCION_PERMISO`); su `TERMINACION` no se duplica, porque ya la anota el ciclo de vida del proceso. `Computador.consultarEventos()` entrega los de un equipo y `ControladorSimulacion.consultarLog()` los mezcla ordenados por ciclo y computador. `Modelo.VistaMetricas` acompaña a cada computador con ciclos, ocupacion, creados, completados, vencidos, cancelados, tiempo de respuesta, equidad de Jain e historial de uso, y calcula `throughput()`, `utilizacion()` y `cumplimiento()`. `Interfaces.PanelPlanificacion` presenta metricas, log y buffers en una sola pestaña, con `Interfaces.GraficaUso` para el historial de uso por computador.

## Pruebas y ramas

Hay dos scripts equivalentes que compilan el proyecto y corren las comprobaciones de
`src/test/java/Pruebas`, sin dependencias externas:

- Windows: `./verificar-buffers.ps1 -Jdk 'C:/Program Files/Java/jdk-22'`
- macOS y Linux: `./verificar-buffers.sh --jdk "$JAVA_HOME"`

Ajustar la ruta al JDK instalado; sirve cualquier JDK 22 o posterior. Verifican capacidad,
FIFO, acceso concurrente, reservas en el anfitrion, rechazos sin cambios, admision de
procesos y reconstruccion CSV. Por defecto corren las 16 comprobaciones que no necesitan
pantalla y devuelven error en la primera que falle. Con `-Interfaz` (o `--interfaz`)
agregan las 6 que crean ventanas Swing: piden un escritorio o un X server, aunque no
muestran la ventana. En un servidor sin escritorio basta envolverlas, por ejemplo
`xvfb-run -a ./verificar-buffers.sh --jdk "$JAVA_HOME" --interfaz`.

GitHub Actions corre lo mismo en cada push a `main` o `develop` y en cada pull request
(`.github/workflows/verificar.yml`): el build de Maven que usa NetBeans, las 16
comprobaciones sin pantalla y las 6 Swing bajo `xvfb-run`. Las capturas que genera la
prueba de observacion quedan como artefacto de la ejecucion.

La pestana **Buffers** permite crear un buffer indicando nombre unico, capacidad y
computador anfitrion. Configurar primero el cluster. La capacidad completa reserva
RAM en ese computador, aun con el buffer vacio; si falta espacio, la creacion se
rechaza sin registrar el buffer. La confirmacion aparece al pie de la ventana y la
ocupacion se puede consultar en Simulacion en vivo. Guardar CSV incluye estos buffers
en la carga inicial. La misma pestana muestra contenido, capacidad, permisos y colas
FIFO de espera, mediante vistas capturadas bajo el bloqueo del anfitrion.

### Productor consumidor local

1. Configurar el cluster y crear un buffer en **Buffers** (por ejemplo Datos, capacidad 1, computador 1).
2. En **Productor / consumidor**, crear un consumidor y un productor del computador 1 asociados a Datos, con intervalo 1, tres elementos y plazo 500.
3. Iniciar el reloj. El consumidor espera por llenos y libera la CPU. Al producir un elemento despierta; cuando falta espacio, el productor espera por vacios.
4. Consultar etapas y avances en Simulacion en vivo; consultar contenido y esperas en Buffers. Al completar ambos procesos, el buffer queda vacio y mantiene su RAM reservada.

Tambien se puede abrir `ejemplos/productor-consumidor-local.csv`: incluye ese flujo
y un CPU-bound en el segundo computador. Para observar acceso entre equipos, abrir
`ejemplos/productor-consumidor-remoto.csv`: productor en el computador 2, buffer y
consumidor en el computador 1, latencia de tres ciclos y otro proceso CPU-bound en el 2.

El intervalo representa instrucciones de trabajo de usuario antes de cada operacion.
A ese trabajo se agregan cinco instrucciones, cada una en un ciclo de sistema:
wait del recurso (vacios o llenos), wait del mutex, insertar o extraer, signal del mutex
y signal del recurso opuesto. El total inicial es `elementos * (intervalo + 5)`;
los ciclos bloqueados descuentan deadline, pero no ejecutan instrucciones.

Cada proceso conserva su etapa y permisos al bloquear o ceder CPU. Un wait que bloquea
ya consumio su instruccion: al recibir el permiso, retoma la etapa siguiente.
Al vencer o cancelarse se retira su espera y se devuelven permisos retenidos; si ya
modifico el contenido, se publica el permiso opuesto sin repetir ni revertir el cambio.
La limpieza del nucleo no agrega ciclos despues de terminar el proceso. El avance
por elementos cuenta operaciones completas, incluyendo sus cinco instrucciones.

`PruebaProductorConsumidorLocal` verifica conservacion de permisos y elementos despues
de cada ciclo, varios productores y consumidores, apropiacion dentro de la region
critica, bloqueos por lleno/vacio, vencimientos en las seis etapas, CSV v1/v2 y reloj
automatico con pausa/reanudacion. Con `-Interfaz` tambien se prueba el flujo de creacion
desde la ventana. Las pruebas no requieren modificar el punto de entrada.

### Acceso remoto a buffers

El computador de un proceso permanece fijo; el buffer puede residir en otro anfitrion.
El anfitrion reserva la RAM del buffer y cada proceso reserva su memoria en su propio
equipo. El algoritmo y los semaforos son los mismos para procesos locales y remotos.

Antes del primer wait de cada operacion remota, el proceso espera la latencia configurada
con motivo RED, sin avanzar PC o MAR ni adquirir permisos. El ciclo de inicio cuenta
como el primero de la espera; puede volver a ejecutarse desde el ciclo posterior al
ultimo ciclo de latencia. La CPU puede seleccionar otro proceso en ese mismo ciclo.
Latencia cero no agrega espera y los accesos locales tampoco la pagan.

La latencia se configura inicialmente en las opciones de Configuracion o durante la
sesion en **Buffers > Aplicar latencia**. Cambiarla afecta accesos nuevos; una espera
ya iniciada conserva su duracion. La carga inicial CSV guarda el valor actualizado.
Las tarjetas muestran el anfitrion remoto y los ciclos de red pendientes.

Cuando una senal entrega un permiso a otro computador, `InterconexionBuffers` lo
conserva hasta que todos los trabajadores confirmen el ciclo. `FinalizadorCiclo` lo
entrega en esa barrera, antes de empezar el siguiente. Asi ningun trabajador necesita
adquirir el mutex de otro computador. Si el receptor vence, el permiso se transfiere
de nuevo; un proceso terminado nunca vuelve a Listo. Los despertares remotos pueden
ejecutar desde el siguiente ciclo, incluso con latencia cero.

Para avanzar manualmente una carga remota se usa `ControladorSimulacion.avanzarCiclo()`:
coordina todos los equipos y la entrega pendiente. Avanzar computadores por separado
no completa la barrera del cluster. El reloj automatico realiza ambas fases.

`PruebaAccesoRemoto` verifica duraciones con latencia 0, 1, 3 y 10; CPU disponible;
cambios en vivo; CSV; vencimientos en red y durante una transferencia; e intercambios
cruzados sobre dos buffers con trabajadores reales. Con `-Interfaz` se comprueban
tambien el control de latencia y sus validaciones.

### Limpieza de productor consumidor

Vencimiento y cancelacion comparten `Computador.terminarProceso`. El controlador
expone `cancelarProceso(proceso)` para solicitar esa ruta sin modificar directamente
colas ni semaforos. Devuelve false si el proceso ya termino. Cancelar no elimina su
definicion de la carga inicial guardable; una carga posterior reconstruye procesos nuevos.

La terminacion retira la espera de semaforo, las transferencias remotas todavia en
cola y la espera de red. Cada permiso tiene un responsable: el nucleo devuelve los
que retiro de la interconexion; el finalizador devuelve los de transferencias que
ya habia extraido, si el receptor termino antes de recibirlas. La cola de transferencias
se modifica bajo su propio semaforo de Java, que se libera antes de acceder a un computador.

La limpieza del contexto es idempotente y rechaza adquisiciones tardias. Devuelve
mutex y permisos retenidos exactamente una vez. Antes de insertar o extraer devuelve
el permiso original; despues de modificar publica el permiso opuesto y conserva el
contenido resultante. Limpieza completa y operacion cancelada son estados diferentes:
un proceso que alcanzo su objetivo no se marca como cancelado. Las tarjetas distinguen
completado de interrumpido y conservan la etapa en la que termino.

`PruebaLimpiezaProductorConsumidor` cubre cancelacion antes y despues de cada instruccion,
vencimientos por etapa con latencia 0 y 2, espera de RAM, red y colas multiples, permiso
de mutex remoto en transito y carreras entre cancelacion y entrega. Comprueba contenido,
RAM, FIFO y que ni permisos ni procesos puedan duplicarse al repetir la limpieza.

### Registro de eventos de buffers

El motor registra produccion, consumo, esperas y despertares de semaforo y red,
accesos remotos, devoluciones de permisos y terminaciones. Cada evento conserva
ciclo, computador, ID de proceso y una secuencia propia del buffer. El historial
retiene los ultimos 256 eventos por buffer e informa cuantos antiguos descarto.
`ControladorSimulacion.consultarEventosBuffers()` entrega copias inmutables para
los visores y la integracion del log; consultar o refrescar no genera eventos.
El registro comienza de nuevo al reconstruir una sesion y no se guarda como estado CSV.

Los acumulados de produccion y consumo se consultan mediante
`ControladorSimulacion.consultarContadoresBuffers()`. Cada vista inmutable captura
los dos contadores y la ocupacion bajo el mismo mutex del buffer, conservando
`producidos - consumidos = cantidad`. Solo una insercion o extraccion exitosa cambia
su contador: los intentos fallidos, consultas, despertares y limpieza no suman.
Si el proceso vence despues de modificar el contenido, ese trabajo real sigue contado
aunque no haya completado las cinco instrucciones de la operacion. Los acumulados
pertenecen a la sesion, son independientes del historial acotado y vuelven a cero al
reconstruir la carga inicial CSV.

Los tiempos del modulo se consultan mediante `consultarBloqueosBuffers()` y
`consultarBloqueosProcesos()` del controlador. Son vistas inmutables; incluyen
procesos terminados, esperas aun abiertas y episodios cerrados por cancelacion o
vencimiento. Se mantienen separados los ciclos y episodios de semaforo y de red.
El promedio es ciclos acumulados dividido entre episodios iniciados, incluyendo los
abiertos con su duracion parcial; sin episodios devuelve cero. No es un promedio
por numero de procesos ni excluye cancelaciones.

Un wait que bloquea ya ejecuto una instruccion en su ciclo: el tiempo de semaforo
empieza a sumar desde el ciclo siguiente y suma tambien el ciclo del despertar o
vencimiento. Una senal recibida en el mismo ciclo del wait puede dar duracion cero.
La latencia de red incluye su ciclo de inicio, segun la convencion del motor remoto.
Pausas y consultas no suman tiempo. Cada nucleo cuenta una vez por ciclo y cierra
el episodio al despertar o limpiar; liberar RAM no elimina los acumulados. Los
totales del buffer se actualizan bajo su mutex, sin adquirir mutex de otros equipos.
Estas APIs entregan datos al modulo de metricas del integrante 3; no calculan sus
indicadores globales ni guardan tiempos de ejecucion en la carga inicial CSV.

### Semaforos simulados

Cada buffer tiene `mutex=1`, `vacios=capacidad` y `llenos=0`. `SemaforoSimulado`
mantiene permisos no negativos y una cola FIFO propia. El semaforo de Java protege
operaciones breves entre hilos reales; esperar por un permiso de AvilaOS devuelve
inmediatamente un resultado, sin detener al trabajador del computador.

El motor solicita `ControladorSimulacion.esperarSemaforo(proceso, semaforo)` con el
proceso en CPU. Si hay permiso, continua en ejecucion. Si no, el nucleo lo pasa a
Bloqueado con motivo SEMAFORO y libera la CPU. `senalizarSemaforo` transfiere un
permiso al primer proceso y lo devuelve a Listo en su propio computador. Ese proceso
ya tiene el permiso concedido y debe continuar con la etapa siguiente, sin repetir
el wait. Una senal sin esperas incrementa el contador; superar su maximo es un error.

El nucleo retira las esperas al terminar un proceso. Si un receptor vence despues
de salir de la cola y antes de despertar, el controlador vuelve a transferir el mismo
permiso. No se adquieren mutex de computadores desde el mutex del semaforo: primero
se retira el receptor y despues se solicita su despertar.

Estas APIs realizan las transiciones; no avanzan PC, MAR ni el reloj. El motor local
las integra con las etapas de las instrucciones, conserva el contexto y limpia los
permisos adquiridos si el proceso termina durante una operacion. `Buffer.insertar` y `extraer` siguen
siendo primitivas de almacenamiento; no actualizan automaticamente los semaforos.

El script de comprobacion incluye permisos iniciales, FIFO, cancelacion, cuatro hilos
concurrentes, bloqueo con CPU disponible, despertar entre computadores, vencimientos
y un intercalado determinista de una senal en transito. Las pruebas de primitivas
usan procesos CPU-bound y las del motor ejecutan productores y consumidores reales.

Estas comprobaciones usan prints y `AssertionError`, como el resto. Las salidas y archivos temporales se escriben en `target`, ignorado por Git.

Son tres escenarios, uno por programa: `PruebaFlujoCompleto` recorre el flujo completo desde las acciones Swing, `PruebaVentanaNormal` comprueba la ventana de 900x680 sin maximizar y `PruebaScrollColas` la estabilidad del scroll. Hasta ahora vivian comentados dentro de `Main.Proyecto1SO2526`, que volvio a ser solo el punto de entrada. El flujo completo carga el ejemplo, crea un sexto proceso desde el formulario, cambia la duracion, inicia/pausa/reanuda/detiene, comprueba finalizacion y RAM, guarda/abre CSV y rechaza un archivo corrupto sin perder la sesion. Al cerrar verifica que no quedan trabajadores del reloj. Los archivos de esta prueba se eligen por ruta desde main, sin automatizar el dialogo del selector.

Todas las ramas de features estan integradas en `main`: las 16 formaban una cadena lineal cuya punta, `feature/planificacion-es`, ya contenia a las demas, por lo que se mezclaron de una sola vez. `develop` apunta al mismo commit. La documentacion describe estos componentes; no sustituye el informe ni el UML de la entrega completa.

### Observacion de buffers

La pestana `Observacion de buffers` separa estado y metricas, perfiles por proceso
(incluidos los terminados) e historial. Las lecturas y el formato automatico se
realizan en un trabajador; el EDT solo presenta el texto. El refresco conserva la
posicion del scroll y no ejecuta operaciones ni agrega eventos al motor. El panel
se compone en Java, como su version anterior; no tiene formulario `.form` asociado.

`ControladorSimulacion.consultarObservacionBuffers()` entrega
`VistaObservacionBuffers`, con arreglos defensivos de records inmutables. Mantiene
la misma sesion mientras captura estado, contadores, bloqueos, procesos y eventos.
El reloj puede avanzar entre estas lecturas: la vista no promete un corte global
atomico durante la ejecucion. Cada contador incluye su propia ocupacion atomica;
para comparar totales entre perfiles y buffers, consultar en pausa o tras la
barrera de `avanzarCiclo()`. No llamar estas consultas desde trabajadores del nucleo.

El historial retiene hasta 256 eventos por buffer y muestra cuantos eventos antiguos
se descartaron; la secuencia es propia de cada buffer. Los contadores acumulados no
se calculan contando el historial y sobreviven a ese descarte. Los tiempos separan
semaforos y red. El promedio divide ciclos por episodios iniciados e incluye la
parte transcurrida de esperas abiertas y los episodios cancelados. Las metricas
pertenecen a la sesion: reiniciar o cargar CSV reconstruye la carga inicial sin
restaurar eventos, contadores ni tiempos anteriores.

Ejecutar cualquiera de los dos scripts con `-Interfaz` o `--interfaz` compila el proyecto
y ejecuta los 22 programas de comprobacion. La prueba integrada de
observacion recorre 72 escenarios locales/remotos con vencimiento y cancelacion en
distintos ciclos; compara eventos con modificaciones reales, perfiles con totales,
lecturas repetidas, copias anteriores y limpieza de esperas. La prueba Swing valida
contenido, lectura asincrona y scroll; genera capturas de estado e historial en
`target/pruebas-buffers`. La validacion visual usa renderizado Swing y no sustituye
la apertura manual del proyecto en NetBeans. Estas APIs quedan disponibles para
el integrante 3; los indicadores globales y su integracion son trabajo separado.
