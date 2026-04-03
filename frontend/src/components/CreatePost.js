import { useState } from "react";
import API from "../services/api";

function CreatePost({ fetchPosts }) {
  const [content, setContent] = useState("");

  const handlePost = async () => {
    if (!content) return;

    try {
      await API.post("/posts", {
        content: content,
        username: "Dhanush",
        timestamp: new Date().toISOString(),
      });

      setContent("");
      fetchPosts();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="card">
      <textarea
        placeholder="What's on your mind?"
        value={content}
        onChange={(e) => setContent(e.target.value)}
      />
      <br />
      <button onClick={handlePost}>Post</button>
    </div>
  );
}

export default CreatePost;