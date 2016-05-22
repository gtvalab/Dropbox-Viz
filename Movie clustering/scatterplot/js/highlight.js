// function checkSelection(d,i) {
//     var sel = document.getSelection();
    
//     if (sel.isCollapsed) return;
//       // The selection doesn't have any content,
//       // i.e., it's just the cursor location,
//       // so don't do anything.
//     var commentBox = document.createElement("div");
//     var commentMarker = document.createElement("span");
//     //create new elements, not currently attached to anything
    
//     d3.select(commentBox)
//         .attr("class", "commentBox")
//         .datum( sel.toString() ) //save for later reference??
//         .text("Comment on \"" + sel.toString() + "\"")
//         .attr("contentEditable", "true")
//         .on("mouseup", stopEvent)
//         .on("keyup", stopEvent);
    
//     d3.select(commentMarker)
//        .attr("class", "commentMarker");
        
//     var split = sel.anchorNode.splitText( sel.anchorOffset );
//     //split the text node containing the start of the selection
//     //into two text nodes, and save the second one
    
//     sel.anchorNode.parentNode.insertBefore(commentMarker, split);  
//     sel.anchorNode.parentNode.insertBefore(commentBox, commentMarker);
//     //insert the comment marker into the paragraph just before
//     //the split point; insert the box just before the marker;
//     //order is important, so that the marker's CSS can be 
//     //dependent on the box's hover/focus state
    
//     sel.anchorNode = split;
//     sel.anchorOffset = 0;
//     //shift the selection to only include content after the split
// }

function checkSelection(d,i) {
    var sel = document.getSelection();
    return sel.toString();
    // console.log(sel.toString());
}


function stopEvent(d,i){
    d3.event.stopPropagation();
}