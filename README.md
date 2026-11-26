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
La configuración de procesos pasará a JSON en su propia rama.

## JSON del sistema de archivos

`Persistencia.ArchivoSistemaArchivos` carga y guarda el disco y el árbol de archivos con Gson.
El formato es el del enunciado, y `ejemplos/sistema-archivos.json` es el ejemplo que trae:

```json
{
  "diskConfig": {"totalBlocks": 200, "initialHead": 50},
  "fileSystem": [
    {"path": "/sistema", "type": "DIRECTORY", "owner": "admin", "permissions": "public"},
    {"path": "/sistema/boot.bin", "type": "FILE", "startBlock": 11, "blocks": 1,
     "owner": "admin", "permissions": "public"}
  ]
}
```

Los campos `swapBlocks`, `process` y `chain` son opcionales y los agrega el programa al guardar.

| Campo | Obligatorio | Significado |
|---|---|---|
| `diskConfig.totalBlocks` | sí | Bloques del disco, entre 100 y 500. |
| `diskConfig.initialHead` | sí | Posición inicial del cabezal. |
| `diskConfig.swapBlocks` | no | Bloques del área swap, que ocupa el final del disco. |
| `path` | sí | Ruta absoluta; el directorio padre debe aparecer en el archivo. |
| `type` | sí | `DIRECTORY` o `FILE`. |
| `owner` | sí | Dueño de la entrada. |
| `permissions` | sí | `public` o `private`. |
| `startBlock`, `blocks` | en archivos | Primer bloque y cantidad de bloques. |
| `process` | no | Proceso que creó el archivo; si falta, `0` (el sistema). |
| `chain` | no | Bloques exactos del archivo, en orden de enlace. |

Al cargar:

- Si falta `swapBlocks`, el área swap usa el tamaño que pide el programa, recortado para que
  todos los archivos queden en el área de datos. En el ejemplo del enunciado el último archivo
  está en el bloque 180, así que el swap queda de 19 bloques.
- Un archivo con `chain` vuelve a esos bloques. Uno sin `chain` empieza en `startBlock` y sigue
  con los bloques libres que vienen después.
- Se rechaza el documento completo, con la ruta del archivo en el mensaje, si el JSON está mal
  formado, falta un campo o tiene un tipo o un permiso desconocido, si dos archivos comparten un
  bloque, si un archivo cae en el área swap o si su directorio no existe.

Al guardar se escriben `swapBlocks`, `process` y `chain`, para que el disco vuelva igual, aun
con archivos fragmentados. El JSON se escribe primero en un temporal de la misma carpeta y luego
se renombra encima del destino: si el guardado falla, el archivo anterior queda intacto.

## Verificación

```bash
./verificar-proyecto2.sh --jdk "$JAVA_HOME"
```

Para incluir las tres comprobaciones Swing (planificación, scroll de colas y la pestaña «Disco y archivos»):

```bash
./verificar-proyecto2.sh --jdk "$JAVA_HOME" --interfaz
```

GitHub Actions compila el mismo proyecto Maven que abre NetBeans y ejecuta ambas rutas.
