$(document).ready(function(){


$('#searchOrder').on('click', function(){
    var email = $('#email').val();
    if ( !validateEmail(email) ){
        alert('Please input emails.');   
        return false; 
    }

    $.ajax({
        type: "POST",
        dataType: "html",
        url: context + "/get-history-order",
        data: {'email': email},
        cache: false,
        beforeSend: function() {
            $('#resultHistoryOrder').empty();
        },
        success: function(data){
            $('#resultHistoryOrder').html( data );
        },
        error: function (request, status, error) {
            //alert(error);
        },
        complete: function(){
        }
    });
    
    console.log(email);
})

});