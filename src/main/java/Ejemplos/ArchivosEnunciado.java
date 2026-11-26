package Ejemplos;

/**
 * Los dos JSON de ejemplo del enunciado, para el boton "Cargar ejemplo". Son los mismos
 * que estan en {@code ejemplos/sistema-archivos.json} y {@code ejemplos/procesos.json};
 * PruebaArchivoProcesos comprueba que no se separen.
 * @author Luciano
 */
public final class ArchivosEnunciado {
    private ArchivosEnunciado() { }

    /** Disco de 200 bloques con el cabezal en 50 y ocho archivos de un bloque. */
    public static final String SISTEMA_ARCHIVOS = """
            {
              "diskConfig": {
                "totalBlocks": 200,
                "initialHead": 50
              },
              "fileSystem": [
                {
                  "path": "/sistema",
                  "type": "DIRECTORY",
                  "owner": "admin",
                  "permissions": "public"
                },
                {
                  "path": "/usuarios",
                  "type": "DIRECTORY",
                  "owner": "admin",
                  "permissions": "public"
                },
                {
                  "path": "/sistema/boot.bin",
                  "type": "FILE",
                  "startBlock": 11,
                  "blocks": 1,
                  "owner": "admin",
                  "permissions": "public"
                },
                {
                  "path": "/usuarios/notas.txt",
                  "type": "FILE",
                  "startBlock": 34,
                  "blocks": 1,
                  "owner": "user1",
                  "permissions": "private"
                },
                {
                  "path": "/sistema/script.py",
                  "type": "FILE",
                  "startBlock": 62,
                  "blocks": 1,
                  "owner": "admin",
                  "permissions": "public"
                },
                {
                  "path": "/sistema/core.dll",
                  "type": "FILE",
                  "startBlock": 64,
                  "blocks": 1,
                  "owner": "admin",
                  "permissions": "public"
                },
                {
                  "path": "/sistema/config.sys",
                  "type": "FILE",
                  "startBlock": 95,
                  "blocks": 1,
                  "owner": "admin",
                  "permissions": "public"
                },
                {
                  "path": "/usuarios/img1.png",
                  "type": "FILE",
                  "startBlock": 119,
                  "blocks": 1,
                  "owner": "user1",
                  "permissions": "private"
                },
                {
                  "path": "/usuarios/data.csv",
                  "type": "FILE",
                  "startBlock": 123,
                  "blocks": 1,
                  "owner": "user2",
                  "permissions": "public"
                },
                {
                  "path": "/usuarios/video.mp4",
                  "type": "FILE",
                  "startBlock": 180,
                  "blocks": 1,
                  "owner": "user1",
                  "permissions": "public"
                }
              ]
            }
            """;

    /** Ocho procesos de archivo del administrador, FCFS, ciclo de 300 ms y 100 bloques de swap. */
    public static final String PROCESOS = """
            {
              "globalConfig": {
                "cycleDurationMs": 300,
                "swapSizeBlocks": 100,
                "initialDiskPolicy": "FIFO"
              },
              "computer": {
                "ramCapacity": 1024,
                "policy": "FCFS",
                "quantum": 0
              },
              "processes": [
                {
                  "name": "Proc_95",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/config.sys",
                  "operation": "READ",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_180",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/video.mp4",
                  "operation": "READ",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_34",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/notas.txt",
                  "operation": "UPDATE",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_119",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/img1.png",
                  "operation": "DELETE",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_11",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/boot.bin",
                  "operation": "READ",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_123",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/data.csv",
                  "operation": "UPDATE",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_62",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/script.py",
                  "operation": "READ",
                  "fileSizeBlocks": 0
                },
                {
                  "name": "Proc_64",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/core.dll",
                  "operation": "READ",
                  "fileSizeBlocks": 0
                }
              ]
            }
            """;
}
