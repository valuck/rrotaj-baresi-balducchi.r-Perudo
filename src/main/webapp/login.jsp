<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<title>Perudo</title>
	</head>
	<body>
		<h1>Login:</h1>
		<form action="/MainServlet" method="get">
            <input type="hidden" name="scope" value="login">

            <% 
            Boolean isClient = (Boolean)request.getAttribute("isClient");
            if (isClient != null && isClient) { %>
                <strong>Address:</strong><br>
                <input name="address" /><br><br>
            <% } %>

            <strong>Port:</strong><br>
            <input name="port" /><br><br>

            <% if (isClient != null && isClient) { %>
                <strong>Username:</strong><br>
                <input name="username" /><br><br>
            <% } %>

            <button type="submit">Confirm</button>
        </form>
	</body>
</html>
