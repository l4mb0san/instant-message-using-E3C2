var express = require("express");
var app = express();
app.use(express.static("public"));
app.set("view engine", "ejs");
app.set("views", "./views");

var server = require("http").Server(app);
var io = require("socket.io")(server);
server.listen(3000);

app.get("/", function(req, res) {
	res.render("index");
});

app.get("/chat", function(req, res) {
	res.render("servicechat");
});

var userlist=[];
io.on("connection", function(socket) {
	console.log(socket.id);

	socket.on("client-send-username", function(data) {
		if(userlist.indexOf(data) >= 0) {
			socket.emit("server-send-register-fail");
		} else {
			userlist.push(data);
			userlist[socket.id] = data;
			socket.username = data;
			socket.emit("server-send-register-success", data);
			io.sockets.emit("server-send-list-online", userlist);
		}
	});

	socket.on("logout", function() {
		userlist.splice(userlist.indexOf(socket.username), 1);
		socket.broadcast.emit("server-send-list-online", userlist);
	});

	socket.on("user-send-message", function(data){
		io.sockets.emit("server-send-message", {
			username: socket.username,
			message: data
		});
	});

	socket.on("typing", function(){
		io.sockets.emit("typing", socket.username + " typing...");
	});

	socket.on("stop", function(){
		io.sockets.emit("stop");
	});

});
