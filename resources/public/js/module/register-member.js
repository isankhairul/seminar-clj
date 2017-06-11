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
    
    

    //$('#modalSignup').modal({backdrop: 'static'});

    event.preventDefault(); // avoid to execute the actual submit of the form.
})

});