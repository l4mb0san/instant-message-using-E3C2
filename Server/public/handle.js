var socket = io ("http://localhost:3000");

$(document).ready(function() {
  $("#loginForm").show();
  $("#chatForm").hide();

  $("#submit").click(function() {
    socket.emit("client-send-username", $("#username").val());
  });

  $("#logout").click(function() {
    $("#chatForm").hide(2000);
    $("#loginForm").show(1000);
    socket.emit("logout");
  });

  $("#send").click(function() {
      socket.emit("user-send-message", $("#message").val());
  });

  $("#message").focusin(function() {
    socket.emit("typing");
  });

  $("#message").focusout(function() {
    socket.emit("stop");
  });

});

socket.on("server-send-register-fail", function() {
  alert("Tên này đã được sử dụng.");
});

socket.on("server-send-register-success", function(data) {
  $("#currentuser").html(data);
  $("#loginForm").hide(2000);
  $("#chatForm").show(2000);
});

socket.on("server-send-list-online", function(data) {
  $("#leftContent").html("");
  data.forEach(function(i) {
    $("#leftContent").append("<div class='user'>" + i + "</div>")
  });
});

socket.on("server-send-message", function(data){
  $("#rightContent").append("<div class='ms'>" + data.username + ": " + data.message + "</div>");
});

socket.on("typing", function(data){
  $("#rightContent").append("<div class='t'>" + data + "</div>");
});

socket.on("stop", function(){
  $(".t").remove();
});
