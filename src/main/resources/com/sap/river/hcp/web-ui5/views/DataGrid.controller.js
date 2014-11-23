sap.ui.controller("views.DataGrid", {
    onInit: function() {
        sap.ui.getCore().getEventBus().subscribe("NAV", "VIEW", jQuery.proxy(this.loadData, this));
    },

    onSelect: function (oEvent) {
        var oTable = this.getView().oTable;
        jQuery.sap.delayedCall(10, null, function() {
            sap.ui.getCore().getModel("view").setProperty("/hasSelection", oTable.getSelectedIndex() !== -1);
        });
    },

    onToolbarPress: function(oEvent, oExtra) {
        var oTable = this.getView().oTable,
            oContext = oTable.getContextByIndex(oTable.getSelectedIndex());

        switch (oExtra.action) {
            case "CREATE":
            case "UPDATE":
                if (oExtra.action === "CREATE") oContext = null;

                sap.ui.getCore().getEventBus().publish("NAV", oExtra.action, {
                    context:sap.ui.getCore().getModel("view").getProperty("/metadata"),
                    data:oContext && oContext.getObject()
                });
                break;
            case "DELETE":
                sap.ui.commons.MessageBox.confirm(
                    utils.Common.getText("MSG_DELETE_CONFIRMATION"),
                    jQuery.proxy(this.deleteRecord, this, oContext.getObject()),
                    utils.Common.getText("TTL_CONFIRMATION")
                );
                break;
            case "REFRESH":
                this.loadData(true);
                break;
        }
    },

    deleteRecord: function(oRecord, bConfirm) {
        if (!bConfirm) return;

        sap.ui.getCore().getModel().remove(
            "/" + sap.ui.getCore().getModel("view").getProperty("/metadata/name") + ___SUFFIX + "(" +
                utils.Common.getKeysValuesFromRecord(oRecord) +  ")",
            null,
            null,
            function(oError) {
                sap.ui.commons.MessageBox.show(oError, "ERROR", utils.Common.getText("TTL_ERROR"), "OK");
            }
        );
    },

    loadData: function() {
        if (arguments.length === 3) {
            var oContext = arguments[2].context;

            sap.ui.getCore().getModel("view").setProperty("/hasSelection", false);
            sap.ui.getCore().getModel("view").setProperty("/metadata", oContext);
            this.getView().oTable
                .removeStyleClass("hidden")
                .bindRows("/" + oContext.name + ___SUFFIX);
        } else if (arguments.length === 1 && arguments[0]) {
            sap.ui.getCore().getModel().refresh();
        }
    }
});