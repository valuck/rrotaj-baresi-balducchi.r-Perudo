<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<title>Perudo</title>
	</head>
	<body>
		<h1><%= (String)request.getAttribute("error") %></h1>
		<form action="/MainServlet" method="get">
			<input type="hidden" name="scope" value="error">
            <button type="submit">Continue</button>
        </form>
	</body>
</html>
