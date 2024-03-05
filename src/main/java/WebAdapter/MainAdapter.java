package WebAdapter;

import Storage.ServerStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.perudo.ServerInterface;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/MainServlet")
public class MainAdapter extends HttpServlet {
    protected static final Logger logger = LogManager.getLogger(MainAdapter.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String responsePage = "index.jsp";
        HttpSession session = request.getSession(true);

        if (ServerInterface.isRunning()) {
            if (session.getAttribute("owner") != null) { //!ServerInterface.getHostAddress().equals(request.getRemoteAddr())) {
                request.setAttribute("error", "Not authorized");
            }

            switch (request.getParameter("scope")) {
                case "error":
                    break;

                case "shutdown": {
                    ServerInterface.shutdown();
                    break;
                }

                case "softshut": {
                    ServerInterface.softShutdown();
                    break;
                }

                case "erase": {
                    ServerStorage.eraseDatabase(true);
                    ServerInterface.softShutdown();
                    break;
                }

                default: {
                    request.setAttribute("error", "Invalid scope");
                }
            }
        } else {
            request.setAttribute("error", "No server found");
        }

        if (request.getAttribute("error") != null)
            responsePage = "error.jsp";

        request.getRequestDispatcher(responsePage).forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String responsePage = "index.jsp";

        HttpSession session = request.getSession(true);
        SessionAdapter currentClient = (SessionAdapter) session.getAttribute("user");
        boolean logged = currentClient != null || session.getAttribute("owner") != null;

        switch (request.getParameter("scope")) {
            case "error":
                break;

            case "setup": {
                boolean isClient = request.getParameter("type").equals("client");
                session.setAttribute("isClient", isClient);
                request.setAttribute("isClient", isClient);
                responsePage = "login.jsp";
                break;
            }

            case "login": { // Lobby sessions are hold by the server on the webapp version to prevent the browser from losing them when closed
                String address = request.getParameter("address");
                int port = 0;

                try {
                    port = Integer.parseInt(request.getParameter("port"));
                } catch (Exception e) { /* Ignore */ }

                if ((boolean) session.getAttribute("isClient")) {
                    if (currentClient == null) { // Mimic a client on the server
                        currentClient = new SessionAdapter(address, ServerInterface.getAccessPort());
                        session.setAttribute("user", currentClient);
                    }

                    if (logged && !currentClient.isRunning())
                        new Thread(currentClient).start();

                    responsePage = "client.jsp";
                } else if (!logged && port > 0) {
                    new Thread(new ServerInterface(port)).start();
                    session.setAttribute("owner", true);
                    responsePage = "server.jsp";
                }

                break;
            }

            default: {
                request.setAttribute("error", "Invalid scope");
            }
        }

        if (request.getAttribute("error") != null)
            responsePage = "error.jsp";

        request.getRequestDispatcher(responsePage).forward(request, response);
    }
}
