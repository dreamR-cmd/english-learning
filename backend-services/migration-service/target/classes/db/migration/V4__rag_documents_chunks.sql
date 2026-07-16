SET NAMES utf8mb4;

CREATE TABLE rag_documents (
  id bigint NOT NULL AUTO_INCREMENT,
  title varchar(255) NOT NULL,
  source varchar(500) DEFAULT NULL,
  content longtext NOT NULL,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_rag_documents_created_at (created_at),
  KEY idx_rag_documents_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rag_document_chunks (
  id bigint NOT NULL AUTO_INCREMENT,
  document_id bigint NOT NULL,
  chunk_index int NOT NULL,
  title varchar(255) NOT NULL,
  source varchar(500) DEFAULT NULL,
  content text NOT NULL,
  vector_id varchar(120) NOT NULL,
  created_at datetime(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rag_chunk_vector_id (vector_id),
  KEY idx_rag_chunk_document_id (document_id),
  CONSTRAINT fk_rag_chunk_document
    FOREIGN KEY (document_id) REFERENCES rag_documents(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
