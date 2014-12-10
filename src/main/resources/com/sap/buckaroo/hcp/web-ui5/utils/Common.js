jQuery.sap.declare("utils.Common");

(function () {
    utils.Common = {
        getText:function () {
            if (!_oBundle) _oBundle = sap.ui.getCore().getModel("i18n").getResourceBundle();
            return _oBundle.getText.apply(_oBundle, arguments);
        },

        getOutlineObject: function(oMetadata) {
            var oOutline = [];

            jQuery.each(oMetadata.dataServices.schema[0].entityType || [], function() {
                if (this.type) return;

                oOutline.push({
                    name: this.name,
                    content: JSON.stringify(this)
                });
            });

            return {outline: oOutline};
        },

        getType: function(sType) {
            switch (sType) {
                case "Edm.SByte":               //  [-][0-9]+
                case "Edm.Int16":               //  [-][0-9]+
                case "Edm.Int32":               //  [-][0-9]+
                    sType = "Integer";
                    break;
                case "Edm.Time":                //  time'<timeLiteral>’
                    sType = "Time";
                    break;
                case "Edm.DateTime":            //  datetime’yyyy-mm-ddThh:mm[:ss[.fffffff]]‘
                case "Edm.DateTimeOffset":      //  datetimeoffset'<dateTimeOffsetLiteral>’
                    sType = "DateTime";
                    break;
                case "Edm.Boolean":             //  true | false
                    sType = "Boolean";
                    break;
                case "Edm.Int64":               //  [-] [0-9]+L
                case "Edm.Byte":                //  [A-Fa-f0-9]+
                case "Edm.Decimal":             //  [0-9]+.[0-9]+M|m
                case "Edm.Double":              //  [0-9]+ ((.[0-9]+) | [E[+ | -][0-9]+])d
                case "Edm.Single":              //  [0-9]+.[0-9]+f
                default:
                    //Edm.Binary                //  binary’[A-Fa-f0-9][A-Fa-f0-9]*’ OR X‘[A-Fa-f0-9][A-Fa-f0-9]*’
                    //Edm.Guid                  //  guid’dddddddd-dddd-dddd-dddd-dddddddddddd’      d = [A-Fa-f0-9]
                    sType = "String";
                    break;
            }
            if (!_mTypesCache[sType]) _mTypesCache[sType] = new sap.ui.model.type[sType]();
            return _mTypesCache[sType];
        },

        getFormatter: function (sType) {
            return function (oValue) {
                return utils.Common.getType(sType).formatValue(oValue, "string");
            };
        },

        getKeysValuesFromRecord: function(oRecord) {
            return (oRecord && oRecord.__metadata && oRecord.__metadata.uri || "").replace(_oUriRegExp, "$1");
        }
    };

    var _oBundle,
        _mTypesCache = {},
        _oUriRegExp = /[^\(]*\((.*)\)$/;
})();