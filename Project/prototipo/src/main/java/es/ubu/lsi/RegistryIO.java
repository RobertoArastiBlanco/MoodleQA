package es.ubu.lsi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RegistryIO {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BARRA =File.separator;
    private static final String CARPETA ="registry";
    private static final String EXCEPTION = "exception";

    private RegistryIO() {
        throw new IllegalStateException("Utility class");
    }

    public static void guardarResultados(String url, String user, String courseid, AnalysisSnapshot instantanea){
        URL netUrl = null;
        String host="";
        FileWriter pw = null;
        try {
            netUrl = new URL(url);
            host = netUrl.getHost();
        } catch (MalformedURLException e) {
            LOGGER.error(EXCEPTION, e);
        }
        try {
            File f1 = new File(CARPETA);
            File f2 = new File(CARPETA + BARRA +host);
            File f3 = new File(CARPETA + BARRA +host+ BARRA +user);
            f1.mkdir();
            f2.mkdir();
            f3.mkdir();
            pw = new FileWriter(CARPETA + BARRA +host+ BARRA +user+ BARRA +courseid+".csv",true);
            pw.append(instantanea.toString());
            pw.flush();
        }catch (Exception e){
            LOGGER.error(EXCEPTION, e);
        }finally {
            try {
                if(pw != null){
                    pw.close();
                }
            } catch (IOException e) {
                LOGGER.error(EXCEPTION, e);
            }
        }
    }

    public static List<AnalysisSnapshot> leerCSV(String url, String user, String courseid){
        URL netUrl = null;
        String host="";
        try {
            netUrl = new URL(url);
            host = netUrl.getHost();
        } catch (MalformedURLException e) {
            LOGGER.error(EXCEPTION, e);
        }
        List<AnalysisSnapshot> registros=new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CARPETA + BARRA +host+ BARRA +user+ BARRA +courseid+".csv"))){
            String linea = br.readLine();
            String[] atributos;
            while (linea != null) {
                atributos=linea.split(";");
                registros.add(new AnalysisSnapshot(atributos));
                linea = br.readLine();
            }
        }catch (Exception e){
            LOGGER.error(EXCEPTION, e);
        }
        return registros;
    }




    public static String generarGraficos(String url,String user,String courseid){
        List<AnalysisSnapshot> listaInformes=leerCSV(url, user, courseid);
        ArrayList<String> lineas=new ArrayList<>();
        StringBuilder grafico=new StringBuilder("Plotly.newPlot(\"grafico\",");
        for (int i=0;i<9;i++){
            lineas.add(generarLinea(listaInformes,i));
        }
        grafico.append(lineas).append(",{title: \"Gráfico de la evolución temporal del curso respecto a roles y perspectivas\",legend:{title:{text:\"Roles-Perspectivas\"}},xaxis: {title:\"Fecha\"},yaxis:{title:\"Desempeño (%)\",tickformat:',.0%',range:[0,1]}},{responsive: true});");
        return grafico.toString();
    }

    public static String generarLinea(List<AnalysisSnapshot> listaInformes, int nroLinea){
        String[] nombresLineas=new String[]{"Diseñador-Pedagógica","Facilitador-Pedagógica","Proveedor-Pedagógica",
                "Diseñador-Tecnológica","Facilitador-Tecnológica","Proveedor-Tecnológica",
                "Diseñador-Estratégica","Facilitador-Estratégica","Proveedor-Estratégica"};
        ArrayList<String> x=new ArrayList<>();
        ArrayList<Float> y=new ArrayList<>();
        StringBuilder linea=new StringBuilder();
        linea.append("{x:");
        for (AnalysisSnapshot instantanea:listaInformes) {
            x.add("new Date("+instantanea.getFecha()+")");
            y.add(instantanea.getPorcentajesDesempeno()[nroLinea]);
        }
        linea.append(x).append(",y:").append(y)
                .append(",mode:\"scatter\", name:\"").append(nombresLineas[nroLinea]).append("\"}");
        return linea.toString();
    }

    public static String informeGlobal(String url,String user){return user+url;}
}
