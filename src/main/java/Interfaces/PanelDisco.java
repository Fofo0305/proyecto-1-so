package Interfaces;

import Modelo.*;
import Sistema.ControladorSimulacion;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * Pestana del disco y del sistema de archivos: arbol de directorios, mapa de bloques con el
 * cabezal, tabla de asignacion, cola de E/S, log, grafica de desplazamiento y metricas, mas
 * los selectores de politica de disco y de sesion y el formulario para pedir operaciones CRUD.
 *
 * Igual que PanelPlanificacion, lee y actua fuera del EDT; el EDT solo pinta la ultima foto.
 * Un intento prohibido se rechaza con su motivo en rojo y la simulacion sigue corriendo.
 * @author Luciano
 */
public final class PanelDisco extends JPanel {

    private static final Color ROJO = new Color(170, 30, 30);

    private final ControladorSimulacion controlador;
    private final JComboBox<PoliticaDisco> politicaDisco = new JComboBox<>(PoliticaDisco.values());
    private final JComboBox<ModoUsuario> modo = new JComboBox<>(ModoUsuario.values());
    private final JTextField usuario = new JTextField("ana", 8);
    private final JLabel sesion = new JLabel("Sesion: -");

    private final DefaultTreeModel modeloArbol = new DefaultTreeModel(new DefaultMutableTreeNode("/"));
    private final JTree arbol = new JTree(modeloArbol);
    private final JLabel infoNodo = new JLabel(" ");
    private final MapaDisco mapa = new MapaDisco();

    private final DefaultTableModel modeloAsignacion = tabla("Archivo", "Ruta", "Bloques", "Primer bloque",
            "Duenio", "Proceso creador", "Etiqueta");
    private final DefaultTableModel modeloCola = tabla("Estado", "Solicitud", "Proceso", "Operacion",
            "Bloque objetivo", "Avance", "Llegada", "Espera", "Politica");
    private final JTextArea logDisco = texto();
    private final JTextArea metricas = texto();
    private final GraficaDesplazamiento grafica = new GraficaDesplazamiento();

    private final JComboBox<OperacionES> operacion = new JComboBox<>(new OperacionES[] {
        OperacionES.CREAR, OperacionES.LEER, OperacionES.ACTUALIZAR, OperacionES.ELIMINAR});
    private final JComboBox<PermisoArchivo> permiso = new JComboBox<>(PermisoArchivo.values());
    private final JTextField ruta = new JTextField("/notas.txt");
    private final JTextField tamano = new JTextField("3");
    private final JTextField nuevoNombre = new JTextField("");
    private final JTextField nombre = new JTextField("Tarea archivo");
    private final JTextField prioridad = new JTextField("1");
    private final JTextField memoria = new JTextField("2");
    private final JTextField instrucciones = new JTextField("4");
    private final JTextField deadline = new JTextField("200");

    private final JLabel resultado = new JLabel("Elija una operacion en «Pedir operacion» o cambie la politica del disco.");
    private final JLabel rechazos = new JLabel(" ");
    private final Semaphore lectura = new Semaphore(1);
    private final Timer timer = new Timer(300, evento -> actualizar());

    private String firmaArbol = "";
    private String firmaAsignacion = "";
    private String firmaCola = "";
    private VistaAlmacenamiento ultima;

    public PanelDisco(ControladorSimulacion controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(barraSuperior(), BorderLayout.NORTH);

        arbol.setRootVisible(true);
        arbol.addTreeSelectionListener(evento -> describirSeleccion());
        infoNodo.setVerticalAlignment(SwingConstants.TOP);
        infoNodo.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
        JPanel izquierda = new JPanel(new BorderLayout(4, 4));
        izquierda.setBorder(BorderFactory.createTitledBorder("Sistema de archivos"));
        izquierda.add(new JScrollPane(arbol), BorderLayout.CENTER);
        infoNodo.setPreferredSize(new Dimension(230, 120));
        izquierda.add(infoNodo, BorderLayout.SOUTH);

        JTabbedPane detalle = new JTabbedPane();
        detalle.addTab("Pedir operacion", formulario());
        JTable asignacion = new JTable(modeloAsignacion);
        asignacion.setDefaultRenderer(Object.class, new ColorPorArchivo());
        detalle.addTab("Tabla de asignacion", new JScrollPane(asignacion));
        JTable cola = new JTable(modeloCola);
        cola.setDefaultRenderer(Object.class, new ResaltarActiva());
        detalle.addTab("Cola de E/S", new JScrollPane(cola));
        detalle.addTab("Log del disco", new JScrollPane(logDisco));
        detalle.addTab("Desplazamiento", grafica);
        detalle.addTab("Metricas", new JScrollPane(metricas));

        JPanel mapaConBorde = new JPanel(new BorderLayout());
        mapaConBorde.setBorder(BorderFactory.createTitledBorder("Disco"));
        mapaConBorde.add(mapa, BorderLayout.CENTER);
        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapaConBorde, detalle);
        derecha.setResizeWeight(0.5);
        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        centro.setDividerLocation(250);
        add(centro, BorderLayout.CENTER);

        JPanel pie = new JPanel(new GridLayout(0, 1, 2, 2));
        pie.add(resultado);
        rechazos.setForeground(ROJO);
        pie.add(rechazos);
        add(pie, BorderLayout.SOUTH);
    }

    private JPanel barraSuperior() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        barra.add(new JLabel("Politica de disco"));
        barra.add(politicaDisco);
        JButton aplicar = new JButton("Aplicar politica de disco");
        barra.add(aplicar);
        aplicar.addActionListener(evento -> {
            PoliticaDisco elegida = (PoliticaDisco) politicaDisco.getSelectedItem();
            trabajar(() -> {
                controlador.cambiarPoliticaDisco(elegida);
                return "El disco atiende con " + elegida + " desde el proximo ciclo.";
            });
        });
        barra.add(Box.createHorizontalStrut(14));
        barra.add(new JLabel("Modo"));
        barra.add(modo);
        barra.add(new JLabel("Usuario"));
        barra.add(usuario);
        modo.addActionListener(evento -> usuario.setEnabled(modo.getSelectedItem() == ModoUsuario.USUARIO));
        usuario.setEnabled(false);
        JButton cambiar = new JButton("Cambiar sesion");
        barra.add(cambiar);
        cambiar.addActionListener(evento -> {
            ModoUsuario elegido = (ModoUsuario) modo.getSelectedItem();
            String nombreUsuario = usuario.getText().trim();
            trabajar(() -> {
                controlador.cambiarSesion(elegido, nombreUsuario);
                return "Los procesos nuevos usan la sesion " + elegido
                        + (elegido == ModoUsuario.USUARIO ? " " + nombreUsuario : "") + ".";
            });
        });
        barra.add(Box.createHorizontalStrut(10));
        sesion.setFont(sesion.getFont().deriveFont(Font.BOLD));
        barra.add(sesion);
        return barra;
    }

    private JPanel formulario() {
        JPanel campos = new JPanel(new GridLayout(0, 4, 8, 6));
        fila(campos, "Operacion", operacion);
        fila(campos, "Ruta", ruta);
        fila(campos, "Tamanio (bloques, crear)", tamano);
        fila(campos, "Permiso (crear)", permiso);
        fila(campos, "Nuevo nombre (actualizar)", nuevoNombre);
        fila(campos, "Nombre del proceso", nombre);
        fila(campos, "Prioridad (0 es la mayor)", prioridad);
        fila(campos, "RAM", memoria);
        fila(campos, "Instrucciones CPU", instrucciones);
        fila(campos, "Deadline desde llegada", deadline);
        JButton crear = new JButton("Crear proceso de archivo");
        crear.addActionListener(evento -> pedirOperacion());
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(campos, BorderLayout.NORTH);
        JPanel boton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        boton.add(crear);
        boton.add(Box.createHorizontalStrut(10));
        boton.add(new JLabel("El proceso usa la sesion actual; el disco lo atiende cuando llega a la CPU."));
        panel.add(boton, BorderLayout.CENTER);
        return panel;
    }

    private static void fila(JPanel campos, String etiqueta, JComponent campo) {
        campos.add(new JLabel(etiqueta));
        campo.setName(etiqueta);
        campos.add(campo);
    }

    private void pedirOperacion() {
        OperacionES elegida = (OperacionES) operacion.getSelectedItem();
        PermisoArchivo visibilidad = (PermisoArchivo) permiso.getSelectedItem();
        String destino = ruta.getText().trim();
        String renombre = nuevoNombre.getText().trim();
        String proceso = nombre.getText().trim();
        String[] numeros = {tamano.getText().trim(), prioridad.getText().trim(), memoria.getText().trim(),
            instrucciones.getText().trim(), deadline.getText().trim()};
        trabajar(() -> {
            // Tamanio y permiso solo cuentan al CREAR; en las otras operaciones el modelo los ignora.
            Proceso creado = controlador.crearProcesoArchivo(proceso, Integer.parseInt(numeros[1]),
                    Integer.parseInt(numeros[2]), Integer.parseInt(numeros[3]), Integer.parseInt(numeros[4]),
                    elegida, destino, Integer.parseInt(numeros[0]), visibilidad,
                    elegida == OperacionES.ACTUALIZAR && !renombre.isEmpty() ? renombre : null);
            return "Creado P" + creado.getId() + ": " + elegida + " " + destino;
        });
    }

    private static DefaultTableModel tabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }

    private static JTextArea texto() {
        JTextArea area = new JTextArea(10, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return area;
    }

    /** Corre la accion fuera del EDT; un rechazo se muestra en rojo con su motivo. */
    private void trabajar(Callable<String> accion) {
        Thread trabajador = new Thread(() -> {
            String mensaje;
            boolean error = true;
            try {
                mensaje = accion.call();
                error = false;
            } catch (SecurityException rechazo) {
                mensaje = "Rechazado: " + rechazo.getMessage() + ". La simulacion sigue.";
            } catch (NumberFormatException formato) {
                mensaje = "Los campos numericos deben ser enteros.";
            } catch (Exception falla) {
                mensaje = falla.getMessage();
            }
            String texto = mensaje;
            boolean rojo = error;
            SwingUtilities.invokeLater(() -> {
                resultado.setForeground(rojo ? ROJO : UIManager.getColor("Label.foreground"));
                resultado.setText(texto);
            });
            actualizar();
        }, "Operacion-disco");
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

    /** Pide una foto del almacenamiento en un trabajador; si ya hay una en curso no hace nada. */
    public void actualizar() {
        if (!lectura.tryAcquire()) return;
        Thread trabajador = new Thread(() -> {
            try {
                VistaAlmacenamiento vista = controlador.consultarAlmacenamiento();
                String textoLog = vista == null ? "" : textoLog(vista);
                String textoMetricas = vista == null ? "" : textoMetricas(vista);
                SwingUtilities.invokeLater(() -> {
                    try {
                        pintar(vista, textoLog, textoMetricas);
                    } finally {
                        lectura.release();
                    }
                });
            } catch (RuntimeException error) {
                SwingUtilities.invokeLater(() -> {
                    resultado.setText("Lectura del disco: " + error.getMessage());
                    lectura.release();
                });
            }
        }, "Leer-disco");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    private void pintar(VistaAlmacenamiento vista, String textoLog, String textoMetricas) {
        ultima = vista;
        mapa.mostrar(vista);
        if (vista == null) {
            sesion.setText("Sesion: -   Configure una simulacion en la pestana Configuracion");
            return;
        }
        sesion.setText("Sesion: " + vista.modo() + " (" + vista.usuario() + ")   Ciclo " + vista.ciclo());
        if (politicaDisco.getSelectedItem() == null) politicaDisco.setSelectedItem(vista.politica());
        pintarArbol(vista.arbol());
        pintarAsignacion(vista.asignacion());
        pintarCola(vista.activa(), vista.cola());
        mostrar(logDisco, textoLog);
        mostrar(metricas, textoMetricas);
        grafica.mostrar(vista.metricas());
        EventoSistema[] ultimos = vista.rechazos();
        if (ultimos.length == 0) {
            rechazos.setText(" ");
        } else {
            EventoSistema evento = ultimos[ultimos.length - 1];
            rechazos.setText("Ultimo rechazo (ciclo " + evento.ciclo() + ", P" + evento.proceso() + "): "
                    + evento.detalle() + "   [" + ultimos.length + " recientes en el log]");
        }
        describirSeleccion();
    }

    private void pintarArbol(VistaEntradaArchivo raiz) {
        StringBuilder firma = new StringBuilder();
        firmar(raiz, firma);
        if (firma.toString().equals(firmaArbol)) return;
        firmaArbol = firma.toString();
        String seleccionada = rutaSeleccionada();
        DefaultMutableTreeNode nodo = nodo(raiz);
        modeloArbol.setRoot(nodo);
        for (int fila = 0; fila < arbol.getRowCount(); fila++) arbol.expandRow(fila);
        if (seleccionada != null) seleccionar(nodo, seleccionada);
    }

    private static void firmar(VistaEntradaArchivo entrada, StringBuilder firma) {
        firma.append(entrada.ruta()).append('|').append(entrada.tamanoBloques()).append('|')
                .append(entrada.dueno()).append('|').append(entrada.permiso()).append('|')
                .append(entrada.primerBloque()).append('\n');
        for (VistaEntradaArchivo hijo : entrada.hijos()) firmar(hijo, firma);
    }

    private static DefaultMutableTreeNode nodo(VistaEntradaArchivo entrada) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(entrada, entrada.esDirectorio());
        for (VistaEntradaArchivo hijo : entrada.hijos()) nodo.add(nodo(hijo));
        return nodo;
    }

    private String rutaSeleccionada() {
        TreePath camino = arbol.getSelectionPath();
        if (camino == null) return null;
        Object dato = ((DefaultMutableTreeNode) camino.getLastPathComponent()).getUserObject();
        return dato instanceof VistaEntradaArchivo entrada ? entrada.ruta() : null;
    }

    private void seleccionar(DefaultMutableTreeNode nodo, String buscada) {
        if (nodo.getUserObject() instanceof VistaEntradaArchivo entrada && entrada.ruta().equals(buscada)) {
            arbol.setSelectionPath(new TreePath(nodo.getPath()));
            return;
        }
        for (int hijo = 0; hijo < nodo.getChildCount(); hijo++)
            seleccionar((DefaultMutableTreeNode) nodo.getChildAt(hijo), buscada);
    }

    /** Muestra los datos del nodo elegido; en un archivo, tambien su cadena de bloques. */
    private void describirSeleccion() {
        TreePath camino = arbol.getSelectionPath();
        Object dato = camino == null ? null : ((DefaultMutableTreeNode) camino.getLastPathComponent()).getUserObject();
        if (!(dato instanceof VistaEntradaArchivo entrada)) {
            infoNodo.setText("<html>Elija un archivo o directorio para ver sus datos.</html>");
            return;
        }
        StringBuilder texto = new StringBuilder("<html><b>").append(html(entrada.ruta())).append("</b><br>")
                .append(entrada.tipo()).append(" de ").append(html(entrada.dueno()))
                .append(", ").append(entrada.permiso());
        if (!entrada.esDirectorio()) {
            texto.append("<br>").append(entrada.tamanoBloques()).append(" bloques, primero el ")
                    .append(entrada.primerBloque()).append("<br>Cadena: ").append(cadena(entrada.primerBloque()));
        } else {
            texto.append("<br>").append(entrada.hijos().length).append(" elementos");
        }
        infoNodo.setText(texto.append("</html>").toString());
    }

    /** Sigue los punteros del disco desde el primer bloque (asignacion encadenada). */
    private String cadena(int primero) {
        if (ultima == null || primero < 0) return "-";
        VistaBloqueDisco[] bloques = ultima.bloques();
        StringBuilder texto = new StringBuilder();
        int actual = primero;
        for (int paso = 0; actual >= 0 && actual < bloques.length && paso < bloques.length; paso++) {
            if (paso > 0) texto.append(" → ");
            if (paso == 12) return texto.append("...").toString();
            texto.append(actual);
            actual = bloques[actual].siguiente();
        }
        return texto.toString();
    }

    private void pintarAsignacion(VistaAsignacionArchivo[] filas) {
        StringBuilder firma = new StringBuilder();
        for (VistaAsignacionArchivo fila : filas) firma.append(fila).append('\n');
        if (firma.toString().equals(firmaAsignacion)) return;
        firmaAsignacion = firma.toString();
        modeloAsignacion.setRowCount(0);
        for (VistaAsignacionArchivo fila : filas)
            modeloAsignacion.addRow(new Object[] {fila.nombre(), fila.ruta(), fila.bloques(), fila.primerBloque(),
                fila.dueno(), "P" + fila.procesoCreador(), fila.etiquetaDisco()});
    }

    private void pintarCola(VistaSolicitudDisco activa, VistaSolicitudDisco[] cola) {
        StringBuilder firma = new StringBuilder().append(activa).append('\n');
        for (VistaSolicitudDisco solicitud : cola) firma.append(solicitud).append('\n');
        if (firma.toString().equals(firmaCola)) return;
        firmaCola = firma.toString();
        modeloCola.setRowCount(0);
        if (activa != null) modeloCola.addRow(filaCola("ATENDIENDO", activa));
        for (VistaSolicitudDisco solicitud : cola) modeloCola.addRow(filaCola("EN COLA", solicitud));
    }

    private static Object[] filaCola(String estado, VistaSolicitudDisco solicitud) {
        return new Object[] {estado, "S" + solicitud.id(), "P" + solicitud.procesoId(), solicitud.operacion(),
            solicitud.bloqueObjetivo(), (solicitud.bloquesTotales() - solicitud.bloquesRestantes()) + "/"
            + solicitud.bloquesTotales(), solicitud.cicloLlegada(), solicitud.tiempoEspera(),
            solicitud.politicaSeleccion() == null ? "-" : solicitud.politicaSeleccion()};
    }

    private static String textoLog(VistaAlmacenamiento vista) {
        StringBuilder texto = new StringBuilder();
        for (EventoSistema evento : vista.eventosDisco())
            texto.append("Ciclo ").append(evento.ciclo()).append(" | ").append(evento.tipo()).append(" | ")
                    .append(evento.detalle().replace('\n', ' ')).append('\n');
        if (vista.rechazos().length > 0) {
            texto.append("\nRechazos recientes del sistema de archivos:\n");
            for (EventoSistema evento : vista.rechazos())
                texto.append("Ciclo ").append(evento.ciclo()).append(" | P").append(evento.proceso()).append(" | ")
                        .append(evento.detalle().replace('\n', ' ')).append('\n');
        }
        return texto.toString();
    }

    private static String textoMetricas(VistaAlmacenamiento vista) {
        VistaMetricasDisco datos = vista.metricas();
        StringBuilder texto = new StringBuilder(String.format(Locale.ROOT,
                "Ciclo %d%nDesplazamiento total del cabezal: %d bloques%nSolicitudes atendidas: %d%n"
                + "Espera promedio: %.2f ciclos%nSuspensiones al swap: %d%nReactivaciones desde el swap: %d%n%n"
                + "Atendidas por politica:%n", datos.ciclo(), datos.desplazamientoTotal(),
                datos.solicitudesAtendidas(), datos.esperaPromedio(), datos.suspensiones(), datos.reactivaciones()));
        for (PoliticaDisco politica : PoliticaDisco.values())
            texto.append(String.format(Locale.ROOT, "  %-6s %d%n", politica, datos.atendidas(politica)));
        int libres = 0;
        int libresSwap = 0;
        for (VistaBloqueDisco bloque : vista.bloques()) {
            if (!bloque.libre()) continue;
            if (bloque.area() == AreaDisco.SWAP) libresSwap++; else libres++;
        }
        texto.append(String.format(Locale.ROOT, "%nBloques libres: %d de datos, %d de swap (swap = %d bloques)%n",
                libres, libresSwap, vista.tamanoSwap()));
        return texto.toString();
    }

    private static String html(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Reemplaza el texto conservando la posicion del scroll. */
    private static void mostrar(JTextArea area, String contenido) {
        if (area.getText().equals(contenido)) return;
        if (!(area.getParent() instanceof JViewport vista)) {
            area.setText(contenido);
            return;
        }
        Point posicion = vista.getViewPosition();
        area.setText(contenido);
        area.setSize(Math.max(area.getWidth(), vista.getWidth()), area.getPreferredSize().height);
        vista.setViewPosition(new Point(posicion.x,
                Math.min(posicion.y, Math.max(0, area.getPreferredSize().height - vista.getHeight()))));
    }

    /** Pinta cada fila con el color que su archivo tiene en el mapa del disco. */
    private final class ColorPorArchivo extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean elegida,
                boolean foco, int fila, int columna) {
            Component celda = super.getTableCellRendererComponent(tabla, valor, elegida, foco, fila, columna);
            if (!elegida) celda.setBackground(MapaDisco.colorDe(String.valueOf(modeloAsignacion.getValueAt(
                    tabla.convertRowIndexToModel(fila), 6))));
            return celda;
        }
    }

    /** La solicitud que el disco atiende va resaltada al tope de la cola. */
    private final class ResaltarActiva extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean elegida,
                boolean foco, int fila, int columna) {
            Component celda = super.getTableCellRendererComponent(tabla, valor, elegida, foco, fila, columna);
            boolean activa = "ATENDIENDO".equals(modeloCola.getValueAt(tabla.convertRowIndexToModel(fila), 0));
            if (!elegida) celda.setBackground(activa ? new Color(255, 226, 180) : Color.WHITE);
            celda.setFont(celda.getFont().deriveFont(activa ? Font.BOLD : Font.PLAIN));
            return celda;
        }
    }
}
