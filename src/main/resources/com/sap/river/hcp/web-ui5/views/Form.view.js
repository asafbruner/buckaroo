sap.ui.jsview("views.Form", {
    getControllerName: function() {
        return "views.Form";
    },

    createContent: function(oController) {
        var oDialog = new sap.ui.commons.Dialog({
            width:"700px",
            keepInWindow:true,
            resizable:false,
            modal:true,
            title:"{/title}",
            buttons:[
                new sap.ui.commons.Button({
                    text:"{/submitBtnText}",
                    press:[oController.onPress, oController]
                }),
                new sap.ui.commons.Button({
                    text:"{i18n>BTN_CANCEL}",
                    press:function () {
                        oDialog.close();
                    }
                })
            ],
            closed: function() { //catch ESC oe 'X' button
                oDialog.destroyContent();
            }
        });

        oDialog.addDelegate({
            onAfterRendering:function () {
                jQuery(".appExplorer .sapUiDlgCont").css("max-height",
                    (jQuery(window).height() - this.getMinSize().height - 50) + "px");
            }
        }, oDialog);

        oDialog.addStyleClass("hidden");

        this.oDialog = oDialog;
        return oDialog;
    }
});