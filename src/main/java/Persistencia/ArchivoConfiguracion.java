package Persistencia;

import EDD.Cola;
import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;
import Modelo.ParametrosES;
import Modelo.PoliticaPlanificacion;
import Modelo.TipoProceso;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Persistencia CSV transitoria de la base del Proyecto 1. @author Adolfo */
public final class ArchivoConfiguracion {
    private ArchivoConfiguracion() { }

    public static void guardar(Path destino, CargaInicial carga) throws IOException {
        if (destino == null || carga == null) throw new IllegalArgumentException("Archivo y carga obligatorios");
        Configuracion configuracion = carga.getConfiguracion();
        StringBuilder texto = new StringBuilder(Csv.registro("AVILAOS", "3"));
        texto.append(Csv.registro("CONFIG", Integer.toString(configuracion.getDuracionMs()),
                Integer.toString(configuracion.getQuantum()), "0"));
        texto.append(Csv.registro("COMPUTADOR", "1", Integer.toString(configuracion.getRam()[0]),
                configuracion.getPoliticas()[0].name(), Integer.toString(configuracion.getQuantums()[0])));
        for (DefinicionProceso proceso : carga.getProcesos()) {
            if (proceso.tipo() == TipoProceso.IO_BOUND) {
                texto.append(Csv.registro("PROCESO", proceso.nombre(), "1", Integer.toString(proceso.prioridad()),
                        Integer.toString(proceso.memoria()), Integer.toString(proceso.instrucciones()),
                        Integer.toString(proceso.deadline()), "IO_BOUND", Integer.toString(proceso.parametrosES().rafaga()),
                        Integer.toString(proceso.parametrosES().duracion())));
            } else {
                texto.append(Csv.registro("PROCESO", proceso.nombre(), "1", Integer.toString(proceso.prioridad()),
                        Integer.toString(proceso.memoria()), Integer.toString(proceso.instrucciones()),
                        Integer.toString(proceso.deadline()), "CPU_BOUND"));
            }
        }
        Path absoluto = destino.toAbsolutePath();
        Path temporal = Files.createTempFile(absoluto.getParent(), "avilaos-", ".tmp");
        try {
            Files.writeString(temporal, texto, StandardCharsets.UTF_8);
            try { Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException error) {
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
        if (cabecera.length != 2 || !"AVILAOS".equals(cabecera[0]) || !"3".equals(cabecera[1]))
            throw new IllegalArgumentException("Linea 1: se requiere formato AVILAOS 3");
        int[] parametros = null;
        int ram = 0;
        PoliticaPlanificacion politica = null;
        int quantumEquipo = 0;
        boolean configLeida = false;
        boolean computadorLeido = false;
        Cola<DefinicionProceso> procesos = new Cola<>();
        for (int i = 1; i < registros.length; i++) {
            String[] campos = registros[i].campos();
            try {
                switch (campos[0]) {
                    case "CONFIG" -> {
                        exigir(campos, 4);
                        if (configLeida) throw new IllegalArgumentException("CONFIG duplicado");
                        parametros = new int[] {entero(campos[1]), entero(campos[2]), entero(campos[3])};
                        if (parametros[2] != 0) throw new IllegalArgumentException("Proyecto 2 no utiliza latencia de red");
                        configLeida = true;
                    }
                    case "COMPUTADOR" -> {
                        exigir(campos, 5);
                        if (computadorLeido) throw new IllegalArgumentException("COMPUTADOR duplicado");
                        if (entero(campos[1]) != 1) throw new IllegalArgumentException("Solo se admite el computador 1");
                        ram = entero(campos[2]);
                        politica = PoliticaPlanificacion.valueOf(campos[3]);
                        quantumEquipo = entero(campos[4]);
                        computadorLeido = true;
                    }
                    case "PROCESO" -> {
                        if (campos.length < 8 || entero(campos[2]) != 1)
                            throw new IllegalArgumentException("Proceso invalido para el computador unico");
                        TipoProceso tipo = TipoProceso.valueOf(campos[7]);
                        if (tipo == TipoProceso.IO_BOUND) {
                            exigir(campos, 10);
                            procesos.agregar(new DefinicionProceso(campos[1], 1, entero(campos[3]), entero(campos[4]),
                                    entero(campos[5]), entero(campos[6]), tipo,
                                    new ParametrosES(entero(campos[8]), entero(campos[9]))));
                        } else {
                            exigir(campos, 8);
                            procesos.agregar(new DefinicionProceso(campos[1], 1, entero(campos[3]), entero(campos[4]),
                                    entero(campos[5]), entero(campos[6])));
                        }
                    }
                    default -> throw new IllegalArgumentException("Registro desconocido: " + campos[0]);
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Linea " + registros[i].linea() + ": " + error.getMessage(), error);
            }
        }
        if (!configLeida || !computadorLeido || ram < 1 || politica == null || quantumEquipo < 1)
            throw new IllegalArgumentException("Falta CONFIG o COMPUTADOR");
        DefinicionProceso[] carga = new DefinicionProceso[procesos.getTamano()];
        for (int i = 0; i < carga.length; i++) carga[i] = procesos.quitar();
        Configuracion configuracion = new Configuracion(new int[] {ram}, parametros[0], parametros[1], 0,
                new PoliticaPlanificacion[] {politica}, new int[] {quantumEquipo});
        return new CargaInicial(configuracion, carga);
    }

    private static void exigir(String[] campos, int cantidad) {
        if (campos.length != cantidad) throw new IllegalArgumentException("Cantidad de campos incorrecta");
    }

    private static int entero(String valor) {
        try { return Integer.parseInt(valor); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Entero invalido: " + valor, error); }
    }
}
