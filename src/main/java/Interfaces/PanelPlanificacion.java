package Interfaces;

import Modelo.*;
import Sistema.ControladorSimulacion;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * Pestana de planificacion, E/S y metricas: cambia la politica de un computador, crea
 * procesos IO_BOUND y muestra metricas, grafica de uso y log global.
 *
 * Las solicitudes y las lecturas se hacen fuera del EDT; el EDT solo presenta el texto.
 * @author Antony
 */
public final class PanelPlanificacion extends JPanel {

    private final ControladorSimulacion controlador;
    private final JTextArea metricas = texto();
    private final JTextArea log = texto();
    private final GraficaUso grafica = new GraficaUso();
    private final JLabel resultado = new JLabel("Configure el cluster antes de aplicar cambios.");
    private final JTextField equipo = new JTextField("1", 4);
    private final JTextField quantum = new JTextField("2", 4);
    private final JComboBox<PoliticaPlanificacion> politica = new JComboBox<>(PoliticaPlanificacion.values());
    private final JTextField[] es = new JTextField[8];
    private final Semaphore lectura = new Semaphore(1);
    private final Timer timer = new Timer(300, evento -> actualizar());

    public PanelPlanificacion(ControladorSimulacion controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JTabbedPane tabs = new JTabbedPane();
        JPanel operaciones = new JPanel(new BorderLayout(8, 12));

        JPanel plan = new JPanel(new FlowLayout(FlowLayout.LEFT));
        plan.add(new JLabel("Computador"));
        plan.add(equipo);
        plan.add(new JLabel("Politica"));
        plan.add(politica);
        plan.add(new JLabel("Quantum"));
        plan.add(quantum);
        JButton aplicar = new JButton("Aplicar politica y quantum");
        plan.add(aplicar);
        aplicar.addActionListener(evento -> {
            String computador = equipo.getText();
            String turno = quantum.getText();
            PoliticaPlanificacion elegida = (PoliticaPlanificacion) politica.getSelectedItem();
            trabajar(() -> {
                controlador.cambiarPolitica(Integer.parseInt(computador.trim()), elegida, Integer.parseInt(turno.trim()));
                return "Politica de C" + computador + ": " + elegida + " quantum=" + turno;
            });
        });
        operaciones.add(plan, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        String[] nombres = {"Nombre", "Computador", "Prioridad (0 es la mayor)", "RAM",
            "Instrucciones CPU totales", "Deadline desde llegada", "Rafaga de CPU", "Duracion E/S en ciclos"};
        String[] valores = {"Tarea E/S", "1", "1", "4", "12", "150", "2", "3"};
        for (int campo = 0; campo < es.length; campo++) {
            formulario.add(new JLabel(nombres[campo]));
            es[campo] = new JTextField(valores[campo]);
            es[campo].setName(nombres[campo]);
            formulario.add(es[campo]);
        }
        JPanel esPanel = new JPanel(new BorderLayout(8, 8));
        esPanel.setBorder(BorderFactory.createTitledBorder("Crear proceso IO_BOUND"));
        esPanel.add(formulario, BorderLayout.CENTER);
        JButton crear = new JButton("Crear proceso de E/S");
        esPanel.add(crear, BorderLayout.SOUTH);
        crear.addActionListener(evento -> {
            String[] campos = new String[es.length];
            for (int campo = 0; campo < campos.length; campo++) {
                campos[campo] = es[campo].getText().trim();
            }
            trabajar(() -> "Creado P" + controlador.crearProcesoES(campos[0],
                    Integer.parseInt(campos[1]), Integer.parseInt(campos[2]), Integer.parseInt(campos[3]),
                    Integer.parseInt(campos[4]), Integer.parseInt(campos[5]), Integer.parseInt(campos[6]),
                    Integer.parseInt(campos[7])).getId());
        });
        operaciones.add(esPanel, BorderLayout.CENTER);
        operaciones.add(new JLabel("Cambios por equipo; EDF y prioridades apropian en el siguiente ciclo. Empates estables."),
                BorderLayout.SOUTH);
        tabs.addTab("Politicas y E/S", new JScrollPane(operaciones));

        JSplitPane comparacion = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(metricas), grafica);
        comparacion.setResizeWeight(0.55);
        tabs.addTab("Metricas y grafica", comparacion);
        tabs.addTab("Log global", new JScrollPane(log));

        add(tabs, BorderLayout.CENTER);
        add(resultado, BorderLayout.SOUTH);
    }

    private static JTextArea texto() {
        JTextArea area = new JTextArea(12, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return area;
    }

    /** Corre la accion fuera del EDT y deja su mensaje al pie de la ventana. */
    private void trabajar(Callable<String> accion) {
        Thread trabajador = new Thread(() -> {
            String mensaje;
            try {
                mensaje = accion.call();
            } catch (NumberFormatException error) {
                mensaje = "Todos los campos numericos deben ser enteros dentro de rango.";
            } catch (Exception error) {
                mensaje = error.getMessage();
            }
            String texto = mensaje;
            SwingUtilities.invokeLater(() -> resultado.setText(texto));
        }, "Operacion-planificacion");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        actualizar();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /** Lee metricas, log y buffers en un trabajador; ignora la llamada si hay otra en curso. */
    public void actualizar() {
        if (!lectura.tryAcquire()) {
            return;
        }
        Thread trabajador = new Thread(() -> {
            try {
                VistaMetricas[] vistas = controlador.consultarMetricas();
                EventoSistema[] eventos = controlador.consultarLog();
                VistaObservacionBuffers buffers = controlador.consultarObservacionBuffers();

                StringBuilder informe = new StringBuilder(
                        "Utilizacion acumulada incluye usuario y operaciones de sistema; sin costo de cambio de contexto.\n");
                long ciclos = 0;
                long ocupados = 0;
                int completos = 0;
                int vencidos = 0;
                int cancelados = 0;
                int creados = 0;
                int respondidos = 0;
                double respuesta = 0;
                double servicio = 0;
                double cuadrados = 0;
                for (VistaMetricas vista : vistas) {
                    informe.append(String.format(Locale.ROOT,
                            "C%d  ciclos=%d  uso=%.2f%%  throughput=%.4f proc/ciclo\n"
                            + "    completados=%d vencidos=%d cancelados=%d respuesta=%.2f (%d respondidos)\n"
                            + "    cumplimiento=%.2f%% equidad Jain=%.4f\n",
                            vista.computador(), vista.ciclos(), vista.utilizacion(), vista.throughput(),
                            vista.completados(), vista.vencidos(), vista.cancelados(), vista.respuesta(),
                            vista.respondidos(), vista.cumplimiento(), vista.equidad()));
                    ciclos = Math.max(ciclos, vista.ciclos());
                    ocupados += vista.ocupados();
                    completos += vista.completados();
                    vencidos += vista.vencidos();
                    cancelados += vista.cancelados();
                    creados += vista.creados();
                    respondidos += vista.respondidos();
                    respuesta += vista.respuesta() * vista.respondidos();
                    servicio += vista.servicio();
                    cuadrados += vista.servicioCuadrado();
                }

                double usoGlobal = ciclos == 0 ? 0 : 100.0 * ocupados / Math.max(1, ciclos * vistas.length);
                double throughputGlobal = ciclos == 0 ? 0 : (double) completos / ciclos;
                double respuestaMedia = respondidos == 0 ? 0 : respuesta / respondidos;
                double cumplimientoGlobal = completos + vencidos == 0 ? 0 : 100.0 * completos / (completos + vencidos);
                double jainGlobal = cuadrados == 0 ? 0 : servicio * servicio / (creados * cuadrados);
                informe.append(String.format(Locale.ROOT,
                        "\nGLOBAL completados=%d vencidos=%d cancelados=%d\n"
                        + "Uso=%.2f%% throughput=%.4f proc/ciclo respuesta=%.2f cumplimiento=%.2f%% Jain=%.4f\n",
                        completos, vencidos, cancelados,
                        usoGlobal, throughputGlobal, respuestaMedia, cumplimientoGlobal, jainGlobal));
                informe.append("Sin muestras: 0. Cumplimiento excluye cancelados y procesos activos.\n")
                        .append("En ejecucion las capturas por equipo pueden corresponder a ciclos vecinos.\n");
                for (VistaContadoresBuffer contador : buffers.contadores()) {
                    informe.append("Buffer ").append(contador.nombre())
                            .append(": producidos=").append(contador.producidos())
                            .append(" consumidos=").append(contador.consumidos()).append('\n');
                }
                for (VistaBloqueosBuffer bloqueo : buffers.bloqueos()) {
                    informe.append("Espera semaforos ").append(bloqueo.nombre())
                            .append(": ciclos=").append(bloqueo.ciclosSemaforo())
                            .append(" promedio=").append(bloqueo.promedioSemaforo()).append('\n');
                }

                StringBuilder bitacora = new StringBuilder(
                        "Ultimos 1024 eventos por computador. Orden: ciclo, computador, secuencia local.\n");
                for (EventoSistema evento : eventos) {
                    bitacora.append("Ciclo ").append(evento.ciclo())
                            .append(" | C").append(evento.computador())
                            .append(" #").append(evento.secuencia())
                            .append(" | P").append(evento.proceso())
                            .append(" | ").append(evento.tipo())
                            .append(" | ").append(evento.detalle().replace("\n", " ").replace("\r", " "))
                            .append('\n');
                }

                SwingUtilities.invokeLater(() -> {
                    try {
                        mostrar(metricas, informe.toString());
                        mostrar(log, bitacora.toString());
                        grafica.mostrar(vistas);
                    } finally {
                        lectura.release();
                    }
                });
            } catch (RuntimeException error) {
                SwingUtilities.invokeLater(() -> {
                    resultado.setText("Lectura: " + error.getMessage());
                    lectura.release();
                });
            }
        }, "Leer-metricas");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    /** Reemplaza el texto conservando la posicion del scroll. */
    private static void mostrar(JTextArea area, String contenido) {
        if (area.getText().equals(contenido)) {
            return;
        }
        JViewport vista = (JViewport) area.getParent();
        Point posicion = vista.getViewPosition();
        area.setText(contenido);
        area.setSize(Math.max(area.getWidth(), vista.getWidth()), area.getPreferredSize().height);
        vista.setViewPosition(new Point(posicion.x,
                Math.min(posicion.y, Math.max(0, area.getPreferredSize().height - vista.getHeight()))));
    }
}
