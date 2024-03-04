<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<title>Perudo</title>
	</head>
	<body>
		<h1>Start as:</h1>
		<form action="/MainServlet" method="get">
            <input type="radio" name="type" value="client" id="client" checked>
            <strong>Client</strong><br><br>

            <input type="radio" name="type" value="server" id="server">
            <strong>Server</strong><br><br>

            <button type="submit" id="submit">Confirm</button>
        </form>
	</body>
</html>
