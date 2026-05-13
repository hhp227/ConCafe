import Foundation
import Shared

enum UserManagementAction {
    case selectFilter(AdminUserFilter)
    case loadMore
    case dismissInfoMessage
}
