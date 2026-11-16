# ÁvilaOS - Proyecto 2

Base del simulador de Sistemas Operativos para incorporar un disco local, un sistema de
archivos jerárquico y suspensión de procesos. La simulación utiliza un único computador
con RAM, CPU, reloj global, procesos CPU-bound e IO-bound y las políticas FCFS, Round
Robin, EDF y Prioridades.

## Ejecutar en NetBeans

1. Instalar y registrar JDK 22 o posterior.
2. Abrir esta carpeta como proyecto Maven.
3. Ejecutar `Main.Proyecto2SO2627`.
4. En Configuración indicar la RAM y la duración del ciclo.
5. Crear procesos o cargar el ejemplo y controlar el reloj con Iniciar, Pausar, Reanudar
   y Detener.

Los archivos `.form` deben editarse con el diseñador de NetBeans. La lógica del sistema
no vive en las ventanas: `ControladorSimulacion` coordina el núcleo y Swing consume vistas
inmutables.

## Componentes conservados del Proyecto 1

- `Modelo`: PCB, estados, configuración, definiciones iniciales y vistas.
- `EDD`: cola enlazada propia protegida por semáforo.
- `Planificacion`: interfaz de política y FCFS, Round Robin, EDF y Prioridades.
- `Sistema.Computador`: CPU, RAM, admisión, colas, E/S temporal, log y métricas.
- `Sistema.RelojGlobal`: un avance por componente y ciclo mediante trabajadores.
- `Sistema.ControladorSimulacion`: API independiente de Swing.
- `Interfaces`: formularios, controles, planificación y observación.

Los buffers, productores, consumidores, acceso remoto y latencia de red del Proyecto 1
fueron retirados porque el enunciado del Proyecto 2 indica que no deben mantenerse.

## Persistencia transitoria

La rama base conserva temporalmente el CSV versión 3 para probar CPU, políticas y E/S.
La persistencia definitiva en dos archivos JSON se incorporará en las ramas de sistema de
archivos y configuración de procesos.

## Verificación

```bash
./verificar-proyecto2.sh --jdk "$JAVA_HOME"
```

Para incluir las dos comprobaciones Swing:

```bash
./verificar-proyecto2.sh --jdk "$JAVA_HOME" --interfaz
```

GitHub Actions compila el mismo proyecto Maven que abre NetBeans y ejecuta ambas rutas.
