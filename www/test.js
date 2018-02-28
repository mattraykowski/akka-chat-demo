$(document).ready(function() {
  var socket = new WebSocket('ws://localhost:9000/socket');
  var isConnected = false;
  var whoAmI = Math.floor(Math.random() * 2048)

  socket.onopen = function(event) {
    console.log('connected', event);
    isConnected = true;
    socket.send(JSON.stringify({ action: 'join', guid: 'guid2' }));
  };

  socket.onclose = function(event) {
    console.log('closed', event);
    isConnected = false;
  };

  socket.onerror = function(event) {
    console.log('error!', event);
  };

  socket.onmessage = function(event) {
    console.log('received message', event);
    $('#message-container').append('<li>' + event.data + '</li>');
  };

  setInterval(function() {
    if (isConnected) {
      socket.send(JSON.stringify({ action: 'sendMessage', guid: 'guid2', message: '(' + whoAmI + ') testing' }));
    }
    
  }, 5000);
});