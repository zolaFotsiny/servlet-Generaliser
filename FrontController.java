
package controller;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import modele.Annotation;
import modele.ModeleView;


public class FrontController extends HttpServlet{

    String marqueur = ".do";
    HashMap<String, Method> methode;
    
    
    /////value parameters anle paramettre
    public String getValuePara(String paramName, Part parte,HttpServletRequest request) throws IOException, ServletException {
        String valueParam=request.getParameter(paramName);
        if (valueParam==null) {   
            valueParam=request.getPart(paramName).getSubmittedFileName();
            String place = this.getInitParameter("place");
            this.Upload(parte, valueParam, place);  
        }
        valueParam=valueParam.trim();
        if (valueParam.isEmpty()) {
             valueParam=null;
        }
        return valueParam;
    }     
        
    ////TOSET
    public void setAttribut (PrintWriter out ,Field[] attributs,String attr,String attrDonne,Class c,Object objet,String valueParam) throws Exception{
        for (Field attribut : attributs) {
            if (attr.compareTo(attribut.getName()) == 0) {
                Method m = c.getMethod("get" + toFirstUpperCase(attr));
                Object attrObject = m.invoke(objet);
                Field donne = attribut.getType().getDeclaredField(attrDonne);
                Method methodSetDonne = attribut.getType().getDeclaredMethod("set" + toFirstUpperCase(attrDonne), donne.getType());
                if (attrObject == null) {
                    Object attrInstance = attribut.getType().newInstance();
                    methodSetDonne.invoke(attrInstance, valueParam);
                    Method methodSetController = c.getMethod("set" + toFirstUpperCase(attr), attribut.getType());
                    methodSetController.invoke(objet, attrInstance);
                } 
                else {
                    out.print("<br>"+methodSetDonne.getReturnType());
                    methodSetDonne.invoke(attrObject, valueParam);
                }
                break;
            }
        }
    }
    ////////////////////controller
     public Class[] getAllClasses(HttpServletRequest req) throws Exception {
        ArrayList<Class> valiny = new ArrayList<>();
        String path = req.getServletContext().getRealPath("/") + "WEB-INF\\classes\\controller";
        path = path.replace("\\", "/");
        try {
            File dir = new File(path);
            File[] liste = dir.listFiles();
            for (File liste1 : liste) {
                Class temp = Class.forName("controller." + liste1.getName().substring(0, liste1.getName().lastIndexOf(".")));
                valiny.add(temp);
            }
            return valiny.toArray(new Class[valiny.size()]);
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }
    
     
    //////dispach modelview sendredirect
    public void dispatch (Method met,Object objet,HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{            ModeleView modele = (ModeleView) met.invoke(objet,request);
            String view = modele.getPage();
            if (modele.getHash() != null) {
                RequestDispatcher disp = request.getRequestDispatcher(view);
                for (Map.Entry dt : modele.getHash().entrySet()) {
                    request.setAttribute((String) dt.getKey(), dt.getValue());
                }
                disp.forward(request, response);
               
            }
            response.sendRedirect(request.getContextPath()+view); 

       
    }
    
     public String toFirstUpperCase(String word) {  
        return  word.substring(0, 1).toUpperCase() + word.substring(1);
    }
     
    public HashMap<String, Method> getAnnotation(HttpServletRequest req) throws Exception {
        HashMap<String, Method> valiny = new HashMap<>();
        Class[] classes = this.getAllClasses(req);
        for (Class a : classes) {
            Method[] alMethode = a.getMethods();
            for (Method alMethode1 : alMethode) {
                if (alMethode1.isAnnotationPresent(Annotation.class)) {
                    Annotation t = alMethode1.getAnnotation(Annotation.class);
                    valiny.put(t.nameClass(), alMethode1);
                }
            }
        }
        return valiny;
    }

    public void Upload(Part part, String nomFichier, String chemin) throws IOException {
        BufferedInputStream entree = null;
        BufferedOutputStream sortie = null;
        int TAILLE_TAMPON = 10240;
        nomFichier = nomFichier.replace("\"", "");
        try {
            entree = new BufferedInputStream(part.getInputStream(), TAILLE_TAMPON);
            sortie = new BufferedOutputStream(new FileOutputStream(new File(chemin + nomFichier)),
                    TAILLE_TAMPON);
            byte[] tampon = new byte[TAILLE_TAMPON];
            int longueur;
            while ((longueur = entree.read(tampon)) > 0) {
                sortie.write(tampon, 0, longueur);
            }
        }
        catch (IOException ignore) {
            ignore.getMessage();
        }
        finally {
            sortie.close();
            entree.close();
        }
    }
    
    

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
				
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String servletpath = request.getServletPath();
        String chemin = servletpath.substring(1, servletpath.length() - marqueur.length());
        if (chemin.contains("/")) {
            String[] tab= chemin.split("/");
            String nomClass = chemin.split("/")[tab.length-2];
            String nomMethod = chemin.split("/")[tab.length-1];
            out.print(nomClass+"****************"+nomMethod);
            try {
                Class classController = Class.forName("controller." +nomClass + "Controller");
                Method classMethod=null;
                Method[] methodes = classController.getMethods();
                for (Method methode1 : methodes) {
                    if (methode1.getName().equals(nomMethod)) {
                        classMethod = methode1;
                    }
                }
                Object objet = classController.newInstance();
                out.print("</br>class objet :"+objet.getClass()+"</br>");
                Field[] attributs = classController.getDeclaredFields();
                Enumeration<String> l = request.getParameterNames();
                List<String> lisst = Collections.list(l);
                out.print("------------------------"+lisst.size()+"<br>");
                if(lisst.size()>0){
                    Collection<Part> parts = request.getParts();
                    for (Part part : parts) {
                       String paramName = part.getName();
                       if (paramName.contains(".")) {
                            String attr = paramName.split("\\.")[0];
                            String attrDonne = paramName.split("\\.")[1];
                            String valueParam=getValuePara(paramName,part,request);
                            setAttribut (out,attributs,attr,attrDonne,classController,objet,valueParam);
                            out.print("</br>"+attrDonne+"  values :"+request.getParameter(paramName));
                        } else {
                            for (Field attribut : attributs) {
                                if (paramName.compareTo(attribut.getName()) == 0) {
                                    out.print("</br>attribut.getName()   "+attribut.getName());
                                    Method methodSetController = classController.getMethod("set" + toFirstUpperCase(paramName), attribut.getType());
                                    methodSetController.invoke(objet, request.getParameter(paramName));
                                }
                            }
                        }
                    }
                }
                if (classMethod.getReturnType().equals(Void.TYPE)) {
                    classMethod.invoke(objet,request);
                } else {
                     dispatch (classMethod,objet,request, response);
                }
            } catch (Exception exce) {
            }
        } else {
            if (methode == null) {
                try {
                    methode = getAnnotation(request);
                } catch (Exception e) {
                }
            }
            try {

                Method met = methode.get(chemin);
                if (met != null) {
                    Class c = met.getDeclaringClass();
                    Object objet = c.newInstance();
                    Field[] attributs = c.getDeclaredFields();
                    Enumeration<String> l = request.getParameterNames();
                    List<String> lisst = Collections.list(l);
                    out.print("------------------------"+lisst.size()+"<br>");
                    if(lisst.size()>0){
                        Collection<Part> parts = request.getParts();
                        for (Part part : parts) {
                            String paramName = part.getName();
                            if (paramName.contains(".")) {
                                String attr = paramName.split("\\.")[0];
                                String attrDonne =paramName.split("\\.")[1];
                                String valueParam=getValuePara(paramName,part,request);
                                setAttribut (out,attributs,attr,attrDonne,c,objet,valueParam);   
                            } else {
                                for (Field attribut : attributs) {
                                    if (paramName.compareTo(attribut.getName()) == 0) {
                                        Method methodSetController = c.getMethod("set" + toFirstUpperCase(paramName), attribut.getType());
                                        methodSetController.invoke(objet, request.getParameter(paramName));
                                    }
                                }
                            }
                        }
                    
                    }
                     if (met.getReturnType().equals(Void.TYPE)) {
                        met.invoke(objet);
                    } else {
                        dispatch (met,objet,request, response);
                    }    
                }
            } catch (Exception e) {
                out.println("error:" + e.getMessage());
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
