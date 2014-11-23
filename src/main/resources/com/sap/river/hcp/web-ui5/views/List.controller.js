sap.ui.controller("views.List", {
    onSelect: function (oEvent) {
        var oNode = oEvent.getParameter("node"),
            oContext = JSON.parse(oNode.getBindingContext("outline").getObject().content);

        sap.ui.getCore().getEventBus().publish("NAV", "VIEW", {context:oContext});
    }
});