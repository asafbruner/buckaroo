sap.ui.controller("views.Form", {
    onInit: function() {
        this.getView().addStyleClass("hidden");

        var loadForm = jQuery.proxy(this.loadForm, this);
        sap.ui.getCore().getEventBus().subscribe("NAV", "CREATE", loadForm);
        sap.ui.getCore().getEventBus().subscribe("NAV", "UPDATE", loadForm);
    },

    onPress: function(oEvent) {
        var oRecord = this.getValidatedData(),
            oDialog = this.getView().oDialog;
        if (this.oContext.action === "CREATE") {
            sap.ui.getCore().getModel().create(
                "/" + this.oContext.metadata.name + ___SUFFIX,
                oRecord,
                null,
                function() {
                    oDialog.close()
                },
                function(oError) {
                    sap.ui.commons.MessageBox.show(oError, "ERROR", utils.Common.getText("TTL_ERROR"), "OK");
                }
            );
        } else { //UPDATE
            sap.ui.getCore().getModel().update(
                "/" + this.oContext.metadata.name + ___SUFFIX + "(" +
                    utils.Common.getKeysValuesFromRecord(this.oContext.data) + ")",
                oRecord,
                null,
                function() {
                    oDialog.close()
                },
                function(oError) {
                    sap.ui.commons.MessageBox.show(oError, "ERROR", utils.Common.getText("TTL_ERROR"), "OK");
                }
            );
        }
    },

    loadForm: function(sChannelId, sEventId, oData) {
        this.oContext = {
            metadata: oData.context || {},
            action: sEventId,
            data: oData.data
        };

        var oFormContainer = new sap.ui.layout.form.FormContainer({}),
            oForm = new sap.ui.layout.form.Form({
                layout:new sap.ui.layout.form.ResponsiveLayout(),
                formContainers:[oFormContainer]
            }),
            aKeys = [],
            oController = this;

        jQuery.each(this.oContext.metadata.key.propertyRef || [], function () {
            aKeys.push(this.name);
        });

        jQuery.each(this.oContext.metadata.property || [], function () {
            oFormContainer.addFormElement(oController.buildControl(this, aKeys));
        });

        oForm.setModel(new sap.ui.model.json.JSONModel(
            oData.data
        ));


        this.getView().oDialog
            .setModel(new sap.ui.model.json.JSONModel({
                title:utils.Common.getText("TTL_" + sEventId + "_RECORD", [oData.context.name]),
                submitBtnText:utils.Common.getText("BTN_" + sEventId)
            }))
            .destroyContent()
            .addContent(oForm)
            .removeStyleClass("hidden")
            .open();

    },

    buildControl: function (oColumn, aKeys) {
        var sName = oColumn.name.replace(/\//g, "."),
            bKey = jQuery.inArray(sName, aKeys) !== -1,
            sIcon = bKey ? "images/key.png" : undefined,
            sType = oColumn.type || "Edm.String",
            bEditableKey = (!bKey || this.oContext.action === "CREATE"),
            oType = utils.Common.getType(oColumn.type),
            oField = null,
            oBinding = {
                path: "/" + oColumn.name,
                type: oType
            },
            bRequired = oColumn.nullable === "false",
            sId = "entityForm--" + sName;


        switch (sType) {
            case "Edm.Date":
                oField = new sap.ui.commons.DatePicker(sId, {
                    value:oBinding,
                    editable:bEditableKey,
                    placeholder:sType,
                    required:!bKey && bRequired
                });
                break;
            case "Edm.Time":
            case "Edm.DateTime":
            case "Edm.DateTimeOffset":
                console.log("not supported control type: " + sType);
                break;
            case "Edm.Boolean":
                oField = new sap.ui.commons.CheckBox(sId, {
                    checked:oBinding,
                    enabled:bEditableKey
                });
                break;

//            case "Edm.Byte":
//            case "Edm.SByte":
//            case "Edm.Int16":
//            case "Edm.Int32":
//            case "Edm.Int64":
//            case "Edm.Decimal":
//            case "Edm.Double":
//            case "Edm.Single":
//                //..
//                break;
//            default:
//                //Edm.Binary
//                //Edm.Guid
//                //Edm.String
//                break;
        }

        if (!oField) {
            oField = new sap.ui.commons.TextField(sId, {
                value:oBinding,
                editable:bEditableKey,
                placeholder:sType,
                required:!bKey && bRequired
            });
        }

        return new sap.ui.layout.form.FormElement({
            label:new sap.ui.commons.Label(sId + "--Label", {
                icon:sIcon,
                text:sName,
                labelFor:oField
            }),
            fields:[oField.setLayoutData(new sap.ui.layout.ResponsiveFlowLayoutData({weight:2}))],
            layoutData:new sap.ui.layout.ResponsiveFlowLayoutData({
                minWidth:350,
                linebreak:true,
                margin:false
            })
        });
    },

    getValidatedData:function () {
        var oData = {},
            bValid = true, oBadInput,
            sName, oInput, oValue, oType,
            sId = "entityForm--";

        jQuery.each(this.oContext.metadata.property || [], function () {
            sName = this.name;
            oInput = sap.ui.getCore().byId(sId + sName.replace(/\//g, "."));

            if (!oInput) return;

            if (oInput instanceof sap.ui.commons.CheckBox) {
                oValue = oInput.getChecked();
                oType = oInput.getBindingInfo("checked").type;
            } else {
                oValue = oInput.getValue();
                oType = oInput.getBindingInfo("value").type;
            }

            try {
                oValue = oType.parseValue(oValue, "string");
                oData[sName] = oValue;
            } catch (oException) {
                if (oInput.setValueState) {
                    oInput.setValueState(sap.ui.core.ValueState.Error);
                    if (!oBadInput) oBadInput = oInput;
                }
                bValid = false;
            }

        });
        if (oBadInput) oBadInput.applyFocusInfo();
        return bValid && oData;
    }
});