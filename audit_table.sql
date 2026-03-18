CREATE TABLE "commons_audit"."filenet_document_management_audit"
(
   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   app_event varchar(256),
   filenet_event varchar(64),
   request_details text,
   app_user varchar(256),
   app_svc_account varchar(256),
   event_time timestamp NOT NULL,
   object_id varchar(64),
   request_status varchar(16),
   error_id varchar(64),
   error_details text
);
/

DROP TABLE "commons_audit"."filenet_document_management_audit";
/
