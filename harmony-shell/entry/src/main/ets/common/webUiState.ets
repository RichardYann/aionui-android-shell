export interface WebUiState {
  navVisible: boolean
  pageLoading: boolean
  errorVisible: boolean
  errorMessage: string
}

export function createWebUiState(): WebUiState {
  return {
    navVisible: true,
    pageLoading: true,
    errorVisible: false,
    errorMessage: ''
  }
}
