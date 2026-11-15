export type CommonResponse<T> = {
  message: string;
  data: T;
};

export type CheckResponse = {
  success: boolean;
  message: string;
};

export type FieldErrorDetail = {
  field: string;
  code: string;
  message: string;
};
