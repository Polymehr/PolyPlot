function hack(obj) {
    var fields = obj.getClass().getDeclaredFields(); 

    var result = {}; 
    for (var i = 0; i < fields.length; ++i) {
        var field = fields[i];
        field.setAccessible(true); 
        Object.defineProperty(result, field.getName(), {
            get: function(obj, field) {
                return function() { return field.get(obj); };
            }(obj, field),
            set: function(obj, field) {
                return function(value) { field.set(obj, value); };
            }(obj, field)
        });
    }
    return result; 
}
