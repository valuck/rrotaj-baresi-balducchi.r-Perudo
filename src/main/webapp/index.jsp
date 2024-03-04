<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<title>Perudo</title>
	</head>
	<body>
		<h1>Start as:</h1>
		<form action="/MainServlet" method="get">
			<input type="hidden" name="scope" value="setup">
		
            <input type="radio" name="type" value="client" checked>
            <strong>Client</strong><br><br>

            <input type="radio" name="type" value="server">
            <strong>Server</strong><br><br>

            <button type="submit">Confirm</button>
        </form>
	</body>
</html>
