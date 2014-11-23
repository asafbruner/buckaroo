sap.ui.jsview("views.DataGrid", {
    getControllerName: function() {
        return "views.DataGrid";
    },

    createContent: function(oController) {
        this.oTable = new sap.ui.table.Table({
            title:"{view>/metadata/name}",
            rowSelectionChange:[oController.onSelect, oController],
            selectionMode:sap.ui.table.SelectionMode.Single,
            selectionBehavior:sap.ui.table.SelectionBehavior.Row,
            busy:"{view>/busy}",
            toolbar: new sap.ui.commons.Toolbar({
                items:[
                    new sap.ui.commons.Button("dataGrid-createBtn", {
                        icon:"sap-icon://add",
                        tooltip:utils.Common.getText("TOL_BUTTON_CREATE"),
                        press:[{action:"CREATE"}, oController.onToolbarPress, oController]
                    }),
                    new sap.ui.commons.ToolbarSeparator({"displayVisualSeparator":false}),
                    new sap.ui.commons.Button("dataGrid-editBtn", {
                        enabled:"{view>/hasSelection}",
                        icon:"sap-icon://edit",
                        tooltip:utils.Common.getText("TOL_BUTTON_EDIT"),
                        press:[{action:"UPDATE"}, oController.onToolbarPress, oController]
                    }),
                    new sap.ui.commons.Button("dataGrid-deleteBtn", {
                        enabled:"{view>/hasSelection}",
                        icon:"sap-icon://delete",
                        tooltip:utils.Common.getText("TOL_BUTTON_DELETE"),
                        press:[{action:"DELETE"}, oController.onToolbarPress, oController]
                    })
                ],
                rightItems:[
                    new sap.ui.commons.Button("dataGrid-refreshBtn", {
                        icon:"sap-icon://refresh",
                        lite:true,
                        tooltip:utils.Common.getText("TOL_REFRESH_DATA"),
                        press:[{action:"REFRESH"}, oController.onToolbarPress, oController]
                    })
                ]

            }),
            columns:{
                path:"view>/metadata/property",
                factory:function (sId, oContext) {
                    var oData = oContext.getObject(),
                        sName = oData.name || "";

                    return new sap.ui.table.Column({
                        width:"100px",
                        label:new sap.ui.commons.Label({
                            icon:oData.isKey ? "images/key.png" : undefined,
                            text:sName.replace(/\//g, ".")
                        }),
                        template:new sap.ui.commons.TextView({
                            text:{
                                path:sName,
                                formatter:utils.Common.getFormatter(oData.type || "String")
                            }
                        }),
                        sortProperty:sName
                    });
                }
            },
            busyIndicatorDelay:0
        });
        this.oTable.addStyleClass("hidden");

        return this.oTable;
    }
});