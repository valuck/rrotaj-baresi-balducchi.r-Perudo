<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<title>Perudo</title>
	</head>
	<body>
		<h1>Server: <%= (String)request.getAttribute("address") %></h1>
		<form action="/MainServlet" method="post">

            <button type="submit">Continue</button>
        </form>
	</body>
</html>
