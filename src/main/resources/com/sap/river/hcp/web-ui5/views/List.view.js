sap.ui.jsview("views.List", {
    getControllerName: function() {
        return "views.List";
    },

    createContent: function(oController) {
        return new sap.ui.commons.Tree({
            showHorizontalScrollbar:true,
            showHeader:false,
            width:"100%",
            nodes:{
                path:"outline>/outline",
                template:new sap.ui.commons.TreeNode({
                    text:"{outline>name}",
                    icon:"images/entity.png"
                })
            },
            select:[oController.onSelect, oController]
        });
    }
});