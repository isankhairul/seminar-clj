$(document).ready(function(){

$('.btn-order').on('click', function(){
    var parent =  $(this).parent().parent();
    var id = $(parent).data('id');
    var tema = $(parent).data('tema');
    
    $('#orderSeminarId').val( id );
    $('#orderTema').text( tema );

    $('#result-order').hide();
    $('#modalOrder').modal({backdrop: 'static'});
    console.log( $(parent).data('tema') );
})

$('#submitOrder').on('click', function(){
    var id = $('#orderSeminarId').val();
    var email = $('#email').val();

    $.ajax({
        type: "POST",
        dataType: "json",
        url: context + "/ajax/order-seminar",
        data: {'seminarId': id, 'email': email},
        cache: false,
        beforeSend: function() {
            $('#result-order').hide();
            $('#progress-order').show();
            $("#form-order input").prop("disabled", true);
            $('#resultOrder').attr('class','alert alert-danger');
        },
        success: function(data){
            var resultStatus = "Failed Order";
            var resultMsg = data.alert;
            
            if(data.status == "success"){
                resultStatus = "Success Order";
                $('#resultOrder').attr('class','alert alert-success');
            }
            
            $('#resultStatus').text( resultStatus );
            $('#resulMsg').text( resultMsg );
            $('#resultOrder').show();
            console.log( data );
        },
        error: function (request, status, error) {
            //alert(error);
        },
        complete: function(){
            $('#progress-order').hide();
            $("#form-order input").prop("disabled", false);
        }
    });
    console.log(email);
})

});