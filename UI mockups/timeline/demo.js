/*eslint-disable */

// create dataset
var data = [];
var names = ["Folders", "Files"];
var endTime = Date.now();
var month = 30 * 24 * 60 * 60 * 1000;
var startTime = endTime - 6 * month;
    
function createEvent (name, maxNbEvents) {
    maxNbEvents = maxNbEvents | 200;
    var event = {
        name: name,
        dates: []
    };
    // add up to 200 events
    var max =  Math.floor(Math.random() * maxNbEvents);
    for (var j = 0; j < max; j++) {
        var time = startTime;
        console.log(time);
        console.log(time + 30*86400000);
        event.dates.push(new Date(time));
        event.dates.push(new Date(time + 30*86400000));
        event.dates.push(new Date(time + 50*86400000));
        event.dates.push(new Date(time + 60*86400000));
    }

    return event;
}
for (var i = 0; i < 2; i++) {
    data.push(createEvent(names[i]));
}

var color = d3.scale.category20();

// create chart function
var eventDropsChart = d3.chart.eventDrops()
    .eventLineColor(function (datum, index) {
        return color(index);
    })
    .start(new Date(startTime))
    .end(new Date(endTime));

// bind data with DOM
var element = d3.select("body").datum(data);

// draw the chart
eventDropsChart(element);