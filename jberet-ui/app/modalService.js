(function () {
    var injectParams = ['$modal'];
    var modalService = function ($modal) {
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
                tempModalDefaults.controller = function ($scope, $modalInstance) {
                    $scope.modalOptions = tempModalOptions;
                    $scope.modalOptions.ok = function (result) {
                        var item = $scope.modalOptions.item;

                        if (item && Object.keys(item).length > 0) {
                            $modalInstance.close($scope.modalOptions.item);
                        } else {
                            $modalInstance.close('ok');
                        }
                    };
                    $scope.modalOptions.close = function (result) {
                        $modalInstance.dismiss('cancel');
                    };
                };
                tempModalDefaults.controller.$inject = ['$scope', '$modalInstance'];
            } else {
                tempModalDefaults.resolve.modalOptions = function () {
                    return tempModalOptions;

                }

            }
            return $modal.open(tempModalDefaults).result;
        };
    };
    modalService.$inject = injectParams;
    angular.module('modal-module', []).service('modalService', modalService)
        .directive('ngEnter', function () {
            return function (scope, element, attrs) {
                element.bind('keydown keypress', function (event) {
                    if (event.which === 13) {
                        scope.$apply(function () {
                            scope.$eval(attrs.ngEnter);
                        });
                        event.preventDefault();
                    }
                });
            };
        });

}());
