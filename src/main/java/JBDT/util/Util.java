package JBDT.util;

import JBDT.data.Pair;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Util {

    public static boolean DEBUG = false;

    public static<U,V> String PairVec2String(ArrayList<Pair<U,V>> al) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < al.size(); i++) {
            Pair<U,V> pair = al.get(i);
            if(i>0) buf.append(",");
            buf.append(pair.first+":"+pair.second);
        }
        return buf.toString();
    }

    public static void debug(String s) {
        if(DEBUG) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            String datetime = dateFormat.format(date);
            System.out.println(datetime+"[DEBUG]:" + s);
        }
    }

    public static void die(String s) {
        System.out.println("ERROR:" + s);
        System.exit(1);
    }

    public static void log(String s) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String datetime = dateFormat.format(date);
        System.out.println(datetime+": "+s);
    }

    public static void log(String s, boolean newline) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String datetime = dateFormat.format(date);
        if(newline)
            System.out.println(datetime+": "+s);
        else
            System.out.print(datetime + ": " + s);
    }

    public static String[] tokenize(String str, String delimiters)
    {
        String[] aa = str.trim().split(delimiters);
        return aa;
    }

    public static double cosine(double[] x, double[] y, int n){
        double xy = 0, xx = 0, yy = 0, x_bar, y_bar, sum_x = 0, sum_y = 0;
        for(int i = 0; i < n; i ++){
            sum_x += x[i];
            sum_y += y[i];
        }
        x_bar = sum_x/n;
        y_bar = sum_y/n;


        for(int i = 0; i < n; i ++){
            xy += (x[i]-x_bar)*(y[i]-y_bar);
            xx += (x[i]-x_bar)*(x[i]-x_bar);
            yy += (y[i]-y_bar)*(y[i]-y_bar);
        }

        return xy / (Math.sqrt(Math.max(xx, 0.0))*Math.sqrt(Math.max(yy, 0.0))+1e-10);
    }

    public static double l2norm(double[] x, int n){
        double xx = 0, x_bar, sum_x = 0;
        for(int i = 0; i < n; i ++){
            sum_x += x[i];
        }
        x_bar = sum_x/n;

        for(int i = 0; i < n; i ++){
            xx += (x[i]-x_bar)*(x[i]-x_bar);
        }

        return Math.sqrt(xx / n);
    }


    public static ArrayList<Integer> divideLoop(int n, int nThreads){
        ArrayList<Integer> seg = new ArrayList<Integer>();
        int t;
        if (n <= nThreads){
            for (int i = 0; i < n+1; i++) {
                seg.add(0);
            }
            for(t = 0; t < n+1; t ++)
                seg.set(t,t);
            return seg;
        }

        for (int i = seg.size(); i < nThreads+1; i++) {
            seg.add(0);
        }

        seg.set(0, 0);
        seg.set(nThreads, n);
        int nTasks = n / nThreads;
        for(t = 1; t < nThreads; t ++)
            seg.set(t,seg.get(t-1)+nTasks);

        return seg;
    }

    /**
     * Find parent node of node path provided
     * @param parent
     * @param path
     * @param l
     * @return
     */
    public static Node findElement(Node parent, ArrayList<String> path, int l) {
        if(parent == null) return null;
        if(l>=path.size()) return parent;
        boolean isFound = false;
        String name = path.get(l);
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType()==Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                parent = node;
                isFound = true;
                break;
            }
        }
        if(!isFound) return null;
        return findElement(parent, path, l+1);
    }


}
