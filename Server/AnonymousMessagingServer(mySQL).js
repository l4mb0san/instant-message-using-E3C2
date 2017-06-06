var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io').listen(server);
var fs = require("fs");
var mysql = require("mysql");

var connectDatabase = mysql.createConnection( {
	host 							: 'localhost',
	user 							: 'root',
	password 					: '',
	database 					: 'anonymous_messaging_app',
});

connectDatabase.connect(function(err) {
	if (err) {
		console.log("Connection to database failed.");
	} else {
		//Nếu kết nối database thành công thì server mới mở port 1209 lắng nghe
		console.log("Connection to database success.");
		server.listen(process.env.PORT || 1209);
	}
});

// Routing
app.set("view engine", "ejs");
app.set("views", "./views");
app.get("/", function(req, res) {
	res.render("monitor");
});


var usersList = [];
var usersSocketIdList = {};

io.on('connection', function (socket) {
	var addedUser = false;
	var avatarName = getFilenameImage(socket.id);
	console.log('User connect to server have socket.id : ', socket.id);

	socket.on('add user', function (username) {
		//Sử dụng để một socket không thể thêm username nhiều lần
		if (addedUser)
			return;
		if (usersList.indexOf(username) > -1) {
			console.log("\nThis name '" + username + "' already exists.");
		} else {
			addedUser = true;
			var avatarPath = 'http://' + getIPAddress() + '/images/' + avatarName;
			console.log(avatarPath);
			var userInfo = {
				SOCKETID 	: socket.id,
				NAME 		: username,
				AVATAR 		: avatarPath
			}
			var userInformation = {
				ID 			: socket.id,
				NAME 		: username,
				AVATAR 		: avatarPath
			}
			usersList.push(username);
			usersSocketIdList[socket.id] = userInformation;
			socket.username = username;
			console.log("\nAdd \'" + socket.username + "\' successfully! -  " + socket.id);


			//Trả về danh sách các user đang kết nối đến server (chỉ cho người vừa kết nối đến)
			socket.emit('login', {
				usersList: usersSocketIdList
			});

			//Trả về tên user vừa kết nối đến (người vừa kết nối đến sẽ không nhận)
			/*- ListActivity -*/
			socket.broadcast.emit('new user joined in ListActivity', {
				newUser: userInformation
			});
			/*- ChatActivity -*/
			socket.broadcast.emit('new user joined in ChatActivity', {
				newUser: userInformation
			});

			//Đưa thông tin vào cơ sở dữ liệu tạm thời
			connectDatabase.query('INSERT INTO temporary_information SET ?',
				userInfo, function(err, res) {
					if (!err) {
						console.log("Add '" + username + "' into database success.");
					} else {
						console.log("Add '" + username + "' into database fail.");
					}
				});
		}
	});

	//-------------------------------------------------------------------------------
	socket.on('client send avatar', function (data) {
        console.log("SERVER SAVED A NEW IMAGE");
        var path = "C:/xampp/htdocs/images/" + avatarName;
        fs.writeFile(path, data);
  	});

	//-------------------------------------------------------------------------------
	socket.on('send PublicKey', function(data) {
		var data = JSON.parse(data);
		var p = data.p;
		var g = data.g;
		var sender = data.sender;
		var reciever = data.reciever;
		var publickey = data.publickey;

		//Trao đổi khóa public key
		/*- ListActivity -*/
		socket.broadcast.to(reciever).emit('publickey exchange in listbox', {
    			sender: 	sender,
    			p: 			p,
    			g: 			g,
    			publickey: 	publickey
    	});
		/*- ChatActivity -*/
		socket.broadcast.to(reciever).emit('publickey exchange in chatbox', {
    			sender: 	sender,
    			p: 			p,
    			g: 			g,
    			publickey: 	publickey
    	});

	});

	//---------------------------------------------------------------------------------
	socket.on('send message', function(data) {
		var data = JSON.parse(data);
		var sender = data.sender;
		var reciever = data.reciever;
		var message = sender + ": "+ data.message;
    	//console.log('sending room post: ', data.room);

    	//Tin nhắn sẽ được đẩy tới client có socket.id tương ứng
    	if (reciever !== undefined || reciever !== null) {
    		connectDatabase.query('SELECT AVATAR FROM temporary_information WHERE SOCKETID = ?',
    			[sender], function(err, rows, fields) {
    				/*- ListActivity -*/
		    		socket.broadcast.to(reciever).emit('notify', {
		    			avatar : rows[0].AVATAR,
		    			sender : sender,
		    			message: message
		    		});
		    		/*- ChatActivity -*/
		    		socket.broadcast.to(reciever).emit('conversation private post', {
		    			avatar : rows[0].AVATAR,
		    			sender: sender,
		    			message: message
		    		});
		    		console.log(" " + message);
    			});
    	}
	});

	//---------------------------------------------------------------------------------
	socket.on('disconnect', function() {
		//Trả về tên user vừa ngắt kết nối (người vừa ngắt kết nối sẽ không nhận)
		/*- ListActivity -*/
		socket.broadcast.emit('user left in ListActivity', {
			userLeft: key
		});
		/*- ChatActivity -*/
		socket.broadcast.emit('user left in ChatActivity', {
			userLeft: key
		});

		var index = 0;
		for (var key in usersSocketIdList) {
		  if (key == socket.id) {
		  	delete usersSocketIdList[key];
		  	usersList.splice(index, 1);
		  	console.log("'" + key + "' diconnected\n");

			//Xóa thông tin người vừa rời khỏi ra database
			connectDatabase.query('DELETE FROM temporary_information WHERE SOCKETID = ?',
				[socket.id], function(err, res) {
					if (!err) {
						console.log("Delete '" + key + "' success.");
					} else {
						console.log("Delete '" + key + "' fail.");
					}
				});
		  	break;
		  }
		  index++;
		}
	});
});

function getFilenameImage(id){
    return id.substring(2) + getMilis() + ".png";
}

function getMilis(){
    var date = new Date();
    var milis = date.getTime();
    return milis;
}

function getIPAddress(){
	var ip = require("ip");
	return ip.address();
}
