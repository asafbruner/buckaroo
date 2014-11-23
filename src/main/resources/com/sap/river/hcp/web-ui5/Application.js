jQuery.sap.declare("Application");
jQuery.sap.require("sap.ui.app.Application");
jQuery.sap.require("utils.Common");

var ___SUFFIX = "s";

sap.ui.app.Application.extend("Application", {
    init:function () {
        var sUrl = "odata/odata.svc";

        sap.ui.getCore().setModel(
            new sap.ui.model.resource.ResourceModel({
                bundleName:"i18n.i18n",
                bundleLocale:"en"
            }),
            "i18n"
        );

        var oDataModel = new sap.ui.model.odata.ODataModel(sUrl, true);
        sap.ui.getCore().setModel(oDataModel);

        var oViewModel = new sap.ui.model.json.JSONModel({
            busy: true,
            hasSelection: false
        });
        sap.ui.getCore().setModel(oViewModel, "view");


        oDataModel
            .attachMetadataLoaded(function () {
                var oOutlineModel = new sap.ui.model.json.JSONModel(
                    utils.Common.getOutlineObject(oDataModel.getServiceMetadata())
                );
                sap.ui.getCore().setModel(oOutlineModel, "outline");
            })
            .attachMetadataFailed(function (oEvent) {
                sap.ui.commons.MessageBox.show(oEvent.getParameter("message"), "ERROR", utils.Common.getText("TTL_ERROR"), "OK");
            })
            .attachRequestSent(function () {
                oViewModel.setProperty("/busy", true);
            })
            .attachRequestCompleted(function () {
                oViewModel.setProperty("/busy", false);
            })
            .attachRequestFailed(function () {
                oViewModel.setProperty("/busy", false);
            });
    },

    main:function () {
        var oRoot = this.getRoot();
        sap.ui.view({type: sap.ui.core.mvc.ViewType.JS, viewName: "views.List"})
            .addStyleClass("list")
            .placeAt(oRoot);
        sap.ui.view({type: sap.ui.core.mvc.ViewType.JS, viewName: "views.DataGrid"})
            .addStyleClass("view")
            .placeAt(oRoot);
        sap.ui.view({type: sap.ui.core.mvc.ViewType.JS, viewName: "views.Form"}).placeAt(oRoot);
    }
});