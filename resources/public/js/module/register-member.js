$(document).ready(function(){
$('.mdb-select').material_select();
// Data Picker Initialization
$('.datepicker').pickadate({
    selectYears: 100,
    max: true,
    formatSubmit: 'yyyy-mm-dd'
});
$.validate({
    form : '#formRegisterMember'
});
$('#formRegisterMember').on('submit', function(event){
    event.preventDefault(); // avoid to execute the actual submit of the form.

    $('#progressRegister').show();
    $('#resultRegister').hide();
    $('#modalSignup').modal({backdrop: 'static'});

    // Get form
    var form = $('#formRegisterMember')[0];

    // Create an FormData object
    var data = new FormData(form);

    $.ajax({
        type: "POST",
        enctype: 'multipart/form-data',
        processData: false,  // Important!
        url: context + "/register-member",
        data: data,
        contentType: false,
        cache: false,
        beforeSend: function() {
            $("#headingProsesRegister").text("Proses Register");
            $("#signup").prop("disabled", true);
            $('#resultRegister').attr('class','alert alert-danger');
        },
        success: function(data){
            var data = JSON.parse( data );
            console.log( data );
            var resultMsg = "";
            if(data.successMessage){
                resultMsg = data.successMessage;
                $('#resultRegister').attr('class','alert alert-success');
            }
            else{
                resultMsg = data.errorMessage.join("<br />");
            }
            $('#resulMsg').html( resultMsg );
        },
        error: function (request, status, error) {
            //alert(error);
        },
        complete: function(){
            $('#progressRegister').hide();
            $("#signup").prop("disabled", false);
            $("#headingProsesRegister").text("Result Register");
            $('#resultRegister').show();
        }
    });

    
})

});