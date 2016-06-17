function hack(obj) {
    var fields = obj.getClass().getDeclaredFields(); 

    var result = {}; 
    for (var i = 0; i < fields.length; ++i) {
        var field = fields[i];
        field.setAccessible(true); 
        Object.defineProperty(result, field.getName(), {
            get: createGetter(obj, fields, i),
            set: createSetter(obj, fields, i)
        });
    }
    return result; 
}

function createGetter(obj, fields, i) {
    return function() { return fields[i].get(obj);}
}

function createSetter(obj, fields, i) {
    return function(value) { return fields[i].set(obj, value);}
}
