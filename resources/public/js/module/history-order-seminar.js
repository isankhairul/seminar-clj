$(document).ready(function(){
$.validate({
    form : '#formSearchOrder'
});

$('#formSearchOrder').on('submit', function(event){
    event.preventDefault();

    var email = $('#email').val();
    $.ajax({
        type: "POST",
        dataType: "html",
        url: context + "/get-history-order",
        data: {'email': email},
        cache: false,
        beforeSend: function() {
            $('#resultHistoryOrder').empty();
            $('#progress').show();
        },
        success: function(data){
            $('#resultHistoryOrder').html( data );
        },
        error: function (request, status, error) {
            //alert(error);
        },
        complete: function(){
            $('#progress').hide();
        }
    });
    
    console.log(email);
})

});