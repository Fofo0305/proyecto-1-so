package Persistencia;

import EDD.Cola;
import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionBuffer;
import Modelo.DefinicionProceso;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Guarda y lee cargas iniciales CSV UTF-8 con versión explícita.
 * @author Adolfo
 */
public final class ArchivoConfiguracion {
    private ArchivoConfiguracion() { }

    public static void guardar(Path destino, CargaInicial carga) throws IOException {
        if (destino == null || carga == null) throw new IllegalArgumentException("Archivo y carga obligatorios");
        Configuracion configuracion = carga.getConfiguracion();
        StringBuilder texto = new StringBuilder(Csv.registro("AVILAOS", "1"));
        texto.append(Csv.registro("CONFIG", Integer.toString(configuracion.getDuracionMs()),
                Integer.toString(configuracion.getQuantum()), Integer.toString(configuracion.getLatencia())));
        int[] ram = configuracion.getRam();
        for (int i = 0; i < ram.length; i++)
            texto.append(Csv.registro("COMPUTADOR", Integer.toString(i + 1), Integer.toString(ram[i]),
                    "FCFS", Integer.toString(configuracion.getQuantum())));
        for (DefinicionProceso proceso : carga.getProcesos())
            texto.append(Csv.registro("PROCESO", proceso.nombre(), Integer.toString(proceso.computador()),
                    Integer.toString(proceso.prioridad()), Integer.toString(proceso.memoria()),
                    Integer.toString(proceso.instrucciones()), Integer.toString(proceso.deadline()), "CPU_BOUND"));
        for (DefinicionBuffer buffer : carga.getBuffers())
            texto.append(Csv.registro("BUFFER", buffer.nombre(), Integer.toString(buffer.capacidad()), Integer.toString(buffer.anfitrion())));
        Path absoluto = destino.toAbsolutePath();
        Path temporal = Files.createTempFile(absoluto.getParent(), "avilaos-", ".tmp");
        try {
            Files.writeString(temporal, texto, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException error) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporal); }
    }

    public static CargaInicial cargar(Path archivo) throws IOException {
        if (archivo == null) throw new IllegalArgumentException("Archivo obligatorio");
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        if (texto.startsWith("\uFEFF")) texto = texto.substring(1);
        RegistroCsv[] registros = Csv.leer(texto);
        if (registros.length == 0) throw new IllegalArgumentException("Archivo vacio");
        String[] cabecera = registros[0].campos();
        if (cabecera.length != 2 || !"AVILAOS".equals(cabecera[0]) || !"1".equals(cabecera[1]))
            throw new IllegalArgumentException("Linea 1: version de archivo no soportada");
        Cola<int[]> computadores = new Cola<>();
        Cola<DefinicionProceso> procesos = new Cola<>();
        Cola<DefinicionBuffer> buffers = new Cola<>();
        int[] parametros = null;
        for (int i = 1; i < registros.length; i++) {
            RegistroCsv registro = registros[i];
            String[] campos = registro.campos();
            try {
                switch (campos[0]) {
                    case "CONFIG" -> {
                        exigirCantidad(campos, 4);
                        if (parametros != null) throw new IllegalArgumentException("CONFIG duplicado");
                        parametros = new int[] {entero(campos[1]), entero(campos[2]), entero(campos[3])};
                    }
                    case "COMPUTADOR" -> {
                        exigirCantidad(campos, 5);
                        if (!"FCFS".equals(campos[3])) throw new IllegalArgumentException("Politica no implementada: " + campos[3]);
                        computadores.agregar(new int[] {entero(campos[1]), entero(campos[2]), entero(campos[4]), registro.linea()});
                    }
                    case "PROCESO" -> {
                        exigirCantidad(campos, 8);
                        if (!"CPU_BOUND".equals(campos[7])) throw new IllegalArgumentException("Tipo de proceso no implementado: " + campos[7]);
                        procesos.agregar(new DefinicionProceso(campos[1], entero(campos[2]), entero(campos[3]),
                                entero(campos[4]), entero(campos[5]), entero(campos[6])));
                    }
                    case "BUFFER" -> {
                        exigirCantidad(campos, 4);
                        buffers.agregar(new DefinicionBuffer(campos[1], entero(campos[2]), entero(campos[3])));
                    }
                    default -> throw new IllegalArgumentException("Registro desconocido: " + campos[0]);
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Linea " + registro.linea() + ": " + error.getMessage(), error);
            }
        }
        if (parametros == null) throw new IllegalArgumentException("Falta el registro CONFIG");
        int[] ram = new int[computadores.getTamano()];
        for (Object elemento : computadores.instantanea()) {
            int[] computador = (int[]) elemento;
            if (computador[0] < 1 || computador[0] > ram.length || ram[computador[0] - 1] != 0
                    || computador[1] < 1 || computador[2] != parametros[1])
                throw new IllegalArgumentException("Linea " + computador[3] + ": ID, RAM o quantum de computador invalido");
            ram[computador[0] - 1] = computador[1];
        }
        DefinicionProceso[] cargaProcesos = new DefinicionProceso[procesos.getTamano()];
        for (int i = 0; i < cargaProcesos.length; i++) cargaProcesos[i] = procesos.quitar();
        DefinicionBuffer[] cargaBuffers = new DefinicionBuffer[buffers.getTamano()];
        for (int i = 0; i < cargaBuffers.length; i++) cargaBuffers[i] = buffers.quitar();
        return new CargaInicial(new Configuracion(ram, parametros[0], parametros[1], parametros[2]), cargaProcesos, cargaBuffers);
    }

    private static void exigirCantidad(String[] campos, int cantidad) {
        if (campos.length != cantidad) throw new IllegalArgumentException("Cantidad de campos incorrecta");
    }

    private static int entero(String valor) {
        try { return Integer.parseInt(valor); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Entero invalido: " + valor, error); }
    }
}
