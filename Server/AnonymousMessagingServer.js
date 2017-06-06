var express = require('express');
var serveIndex = require('serve-index');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io').listen(server);
var fs = require("fs");
var path = require('path');
var pg = require('pg');

var config = {
	host 							: 'ec2-23-23-227-188.compute-1.amazonaws.com', // Server hosting the postgres database
	user 							: 'gawfhacsjaisfn', //env var: PGUSER
	password 					: '0d189d6e719ac80c707ad386e7c70bee96eeab3172044cf2fc39d5d3578cd0a9', //env var: PGPASSWORD
	database 					: 'd2gkheathvkd56', //env var: PGDATABASE
	port 							: 5432, //env var: PGPORT
	max 							: 10, // max number of clients in the pool
	idleTimeoutMillis	: 30000, // how long a client is allowed to remain idle before being closed
	ssl 							: true
};

//this initializes a connection pool
//it will keep idle connections open for 30 seconds
//and set a limit of maximum 10 idle clients
const pool = new pg.Pool(config);

pool.on('error', function (err, client) {
  console.error('idle client error', err.message, err.stack);
});

module.exports.query = function (text, values, callback) {
  console.log('query:', text, values);
  return pool.query(text, values, callback);
};

module.exports.connect = function (callback) {
  return pool.connect(callback);
};

pool.connect(function(err, client, done) {
  if(err) {
    return console.error('error fetching client from pool: ', err);
  }
	//Nếu kết nối database thành công thì server mới mở port 1209 lắng nghe
	console.log("Connection to database success.");
	server.listen(process.env.PORT || 1209);
});

// Routing
app.set("view engine", "ejs");
app.set("views", "./views");
app.get("/", function(req, res) {
	res.render("monitor");
});
app.use('/images', serveIndex('public/images', {'icons': true}));
app.use(express.static('public'));


var usersList = [];
var usersSocketIdList = {};

io.on('connection', function (socket) {
	var addedUser = false;
	var avatarName = getFilenameImage(socket.id);
	var avatarPath = "https://" + "anonymous-messaging-app.herokuapp.com/images/" + avatarName;
	console.log('User connect to server have socket.id : ', socket.id);

	//----------------ADD NEW USER------------------------------------------------
	socket.on('add user', function (username) {
		//Sử dụng để một socket không thể thêm username nhiều lần
		if (addedUser)
			return;
		if (usersList.indexOf(username) > -1) {
			console.log("\nThis name '" + username + "' already exists.");
		} else {
			addedUser = true;

			//Kiểm tra xem đã upload avatar chưa
			if (!checkUploadAvatar(path.join(__dirname, 'public', 'images', avatarName))){
				var dir = path.join(__dirname, 'public', 'images', 'animal');
				avatarName = randomFileFromDir(dir);
				avatarPath = "https://" + "anonymous-messaging-app.herokuapp.com/images/animal/" + avatarName;
				console.log("Random a image from Animal Folder: " + avatarName);
			}

			var userInformation = {
				ID 				: socket.id,
				NAME 			: username,
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
			insertInforToDatabase(socket.id, username, avatarPath);
		}
	});

	//----------------CLIENT SEND AVATAR------------------------------------------
	socket.on('client send avatar', function (data) {
			var _path = path.join(__dirname, 'public', 'images', avatarName);
      console.log("SERVER SAVED A NEW IMAGE IN '" + _path + "'");
      fs.writeFile(_path, data);
  	});

	//----------------SEND PUBLICKEY----------------------------------------------
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

	//----------------SEND MESSAGE------------------------------------------------
	socket.on('send message', function(data) {
		var data = JSON.parse(data);
		var sender = data.sender;
		var reciever = data.reciever;
		var message = sender + ": "+ data.message;

		//Tin nhắn sẽ được đẩy tới client có socket.id tương ứng
		sendMessage(socket, reciever, sender, message);
	});

	//----------------DISCONECT---------------------------------------------------
	socket.on('disconnect', function() {
		//Trả về tên user vừa ngắt kết nối (người vừa ngắt kết nối sẽ không nhận)
		/*- ListActivity -*/
		socket.broadcast.emit('user left in ListActivity', {
			userLeft: socket.id
		});
		/*- ChatActivity -*/
		socket.broadcast.emit('user left in ChatActivity', {
			userLeft: socket.id
		});

		//Xóa ảnh trong đường dẫn
		var _path = path.join(__dirname, 'public', 'images', avatarName);
		deleteFile(_path);
		removeInforFromDatabase(socket.id);

	});

});

//------------------FUNCTION----------------------------------------------------
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

function deleteFile(_path){
	if (checkUploadAvatar(_path)) {
		fs.unlink(_path, (err) => {
			if (err) {
				console.log('fail deleted ' + _path);
				return false;
			} else {
				console.log('successfully deleted ' + _path);
			}
		});
	}
	return true;
}

function insertInforToDatabase(_socketid, _username, _avatarPath){
	pool.connect(function(err, client, done) {
		if(err) {
			console.error('error fetching client from pool', err);
			return false;
		} else {
			client.query("INSERT INTO temporary_information VALUES ('" + _socketid + "', '" + _username + "', '" + _avatarPath + "')", function(err, result) {
				done(err);
				if(err) {
					console.log("Add '" + _username + "' into database fail.");
					return false;
				} else {
					console.log("Add '" + _username + "' into database success.");
				}
			});
		}
	});
	return true;
}

function removeInforFromDatabase(_socketid){
	var index = 0;
	for (var key in usersSocketIdList) {
		if (key === _socketid) {
			delete usersSocketIdList[key];
			usersList.splice(index, 1);
			console.log("'" + key + "' diconnected\n");

			//Xóa thông tin người vừa rời khỏi ra database
			pool.connect(function(err, client, done) {
				if(err) {
					console.error('error fetching client from pool', err);
					return false;
				} else {
					client.query("DELETE FROM temporary_information WHERE \"SOCKETID\" = '" + key + "'", function(err, result) {
					 done(err);
					 if(err) {
						 console.log("Delete '" + key + "' fail.");
						 return false;
					 } else {
						 console.log("Delete '" + key + "' success.");
					 }
				 });
				}
			});
		}
		index++;
	}
	return true;
}

function sendMessage(socket, reciever, sender, message){
	if (reciever !== undefined || reciever !== null) {
		pool.connect(function(err, client, done) {
			if(err) {
				console.error('error fetching client from pool', err);
				return false;
			} else {
				client.query("SELECT \"AVATAR\" FROM temporary_information WHERE \"SOCKETID\" = '" + sender + "'", function(err, result) {
					done(err);
					if(err) {
						console.error('error running query', err);
						return false;
					} else {
						/*- ListActivity -*/
						socket.broadcast.to(reciever).emit('notify', {
							avatar : result.rows[0].AVATAR,
							sender : sender,
							message: message
						});
						/*- ChatActivity -*/
						socket.broadcast.to(reciever).emit('conversation private post', {
							avatar : result.rows[0].AVATAR,
							sender: sender,
							message: message
						});
						console.log(" " + message);
					}
				});
			}
		});
	}
	return true;
}

function checkUploadAvatar(_path){
	return fs.existsSync(_path);
}

function randomFileFromDir(_dir){
	var arr = [];
	var value = null;
	var files = fs.readdirSync(_dir);
	for (var i in files) {
	  arr.push(files[i]);
	}
	return arr[Math.floor(Math.random()*arr.length)];
}
