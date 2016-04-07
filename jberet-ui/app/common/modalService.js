'use strict';

require('ui-bootstrap-tpls');

var modalService = function ($uibModal) {
    var modalDefaults = {
        backdrop: true,
        keyboard: true,
        modalFade: true,
        templateUrl: 'template/modal/confirmation.html'
    };

    var modalOptions = {
        closeButtonText: 'Cancel',
        actionButtonText: 'OK',
        headerText: 'JBeret UI',
        bodyText: 'Are you sure?'
    };

    this.showModal = function (customModalDefaults, customModalOptions) {
        if (!customModalDefaults) customModalDefaults = {};
        customModalDefaults.backdrop = 'static';
        return this.show(customModalDefaults, customModalOptions);
    };

    this.show = function (customModalDefaults, customModalOptions) {
        var tempModalDefaults = {};
        var tempModalOptions = {};
        angular.extend(tempModalDefaults, modalDefaults, customModalDefaults);
        angular.extend(tempModalOptions, modalOptions, customModalOptions);
        if (!tempModalDefaults.controller) {
            tempModalDefaults.controller = function ($scope, $uibModalInstance) {
                $scope.modalOptions = tempModalOptions;
                $scope.modalOptions.ok = function (result) {
                    if(customModalOptions.validate) {
                        $scope.modalOptions.invalid = {};
                        if(!customModalOptions.validate($scope.modalOptions)) {
                            return;
                        }
                    }
                    var item = $scope.modalOptions.item;

                    if (item && Object.keys(item).length > 0) {
                        $uibModalInstance.close($scope.modalOptions.item);
                    } else {
                        $uibModalInstance.close(true);
                    }
                };
                $scope.modalOptions.close = function (result) {
                    $uibModalInstance.dismiss('cancel');
                };
            };
            tempModalDefaults.controller.$inject = ['$scope', '$uibModalInstance'];
        } else {
            tempModalDefaults.resolve.modalOptions = function () {
                return tempModalOptions;
            };
        }
        return $uibModal.open(tempModalDefaults).result;
    };
};

module.exports = modalService;
