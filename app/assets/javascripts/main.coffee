getISODateTime = (d) ->
    s = (a,b) -> (1e15+a+"").slice(-b)
    
    if (typeof d == 'undefined') 
    	d = new Date()    

    d.getFullYear() + '-' +
        s(d.getMonth()+1,2) + '-' +
        s(d.getDate(),2) + ' ' +
        s(d.getHours(),2) + ':' +
        s(d.getMinutes(),2) + ':' +
        s(d.getSeconds(),2) + d.getTimezoneOffset()




$ ->
	
    $('#calendar').fullCalendar
        header:
            left: 'prev,next today'
            center: 'title'
            right: 'month,agendaWeek,agendaDay'
        defaultView: 'agendaWeek'
        height: 600
        firstDay: 1
        editable: true
        timeFormat: 'HH:mm'
        axisFormat: 'HH:mm'
        
        events: (start, end, callback) ->
            $.getJSON('/events/'+getISODateTime(start)+'/'+getISODateTime(end),
                (events) -> callback(events)
            )
        dayClick: (date, allDay, jsEvent, view) ->         	
        	window.location = '/events/new?start='+date.toISOString()
        eventClick: (event, jsEvent, view) ->
        	window.location = '/events/edit/'+event.id
        eventDrop: (event,dayDelta,minuteDelta,allDay,revertFunc) ->
        	$.post '/events/move/'+event.id, 
                    {dayDelta: dayDelta
                    minuteDelta: minuteDelta
                    allDay: allDay},
                    (data, textStatus, jqXHR) -> alert(data)
        eventResize: (event, dayDelta, minuteDelta, revertFunc, jsEvent, ui, view) ->
        	$.post '/events/resize/'+event.id, 
        	        {dayDelta: dayDelta
        	        minuteDelta: minuteDelta},
        	        (data, textStatus, jqXHR) -> alert(data)
        	

    
    $('.datepicker').datepicker
        weekStart: 1
        autoclose: true
    $('.timepicker-default').timepicker
        defaultTime: 'value'
        showMeridian: false 
    $('#allDay').change(showHideTimePickers) 
    
    showHideTimePickers()

showHideTimePickers = ->         
    if ($('#allDay').is(':checked'))  
        $('#startTime_field').hide() 
        $('#endTime_field').hide()
    else 
        $('#startTime_field').show() 
        $('#endTime_field').show()

    
    

