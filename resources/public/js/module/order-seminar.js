$(document).ready(function(){
$.validate({
    form : '#formOrder'
});
$('.btn-order').on('click', function(){
    var parent =  $(this).parent().parent();
    var id = $(parent).data('id');
    var tema = $(parent).data('tema');
    
    $('#orderSeminarId').val( id );
    $('#orderTema').text( tema );

    $('#resultOrder').hide();
    $('#modalOrder').modal({backdrop: 'static'});
    console.log( $(parent).data('tema') );
})

$('#formOrder').on('submit', function(event){
    event.preventDefault();
    
    var id = $('#orderSeminarId').val();
    var email = $('#email').val();
    
    $.ajax({
        type: "POST",
        dataType: "json",
        url: context + "/order-seminar",
        data: {'seminar_id': id, 'email': email},
        cache: false,
        beforeSend: function() {
            
            $('#progress-order').show();
            $("#form-order input").prop("disabled", true);
            $('#resultOrder').attr('class','alert alert-danger');
        },
        success: function(data){
            var resultStatus = "Failed Order";
            var resultMsg = data.alert;
            
            if(data.status == "success"){
                resultStatus = "Success Order";
                resultMsg = "Serial Number: " + data.serial ;
                $('#resultOrder').attr('class','alert alert-success');
            }
            $('#resultStatus').text( resultStatus );
            $('#resulMsg').text( resultMsg );
            console.log( data );
        },
        error: function (request, status, error) {
            //alert(error);
        },
        complete: function(){
            $('#progress-order').hide();
            $("#form-order input").prop("disabled", false);
            $('#resultOrder').show();
        }
    });
    
    console.log(email);
})

});